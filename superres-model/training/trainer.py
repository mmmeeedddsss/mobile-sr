from argparse import ArgumentParser
from itertools import count
import os

import tensorflow as tf
import numpy as np

from data import create_combined_data_loader
from losses import *
from models import * 
from schedulers import *
import train_opts as OPTS

def parse_arguments():
    parser = ArgumentParser()
    parser.add_argument(
        'training_file',
        help='.tfrecord file storing png hr-lr pairs for training')
    parser.add_argument(
        'validation_file',
        help='.tfrecord file storing png hr-lr pairs for validation')
    parser.add_argument(
        '--learning-rate', '-l',
        help='learning rate for the optimizer. Defaults to 1e-3',
        type=float,
        default=1e-3)
    parser.add_argument(
        '--discr-learning-rate', '-dl',
        help='learning rate for the discriminator\'s optimizer. Defaults to 1e-3',
        type=float,
        default=1e-3)
    parser.add_argument(
        '--batch-size', '-b',
        help='batch size to use during training. Defaults to 4.',
        type=int,
        default=4)
    parser.add_argument(
        '--num-epochs', '-n',
        help='number of training epochs. Defaults to 1.',
        type=int,
        default=1)
    arguments = parser.parse_args()
    return arguments

def summarize_collection(loss_collection):
    """ a small function to add a loss collection to the summary """
    for tensor in loss_collection:
        name, _ = tensor.name.split(':') # get the left of the ':' in the name
        tf.summary.scalar(name, tensor)

def format_loss(loss_collection, loss_values):
    """ a small function that formats a loss string based on a collection for printing """
    strings = []
    total_loss = 0
    for tensor, val in zip(loss_collection, loss_values):
        total_loss += val # add to total loss
        name, _ = tensor.name.split(':') # get the left of the ':' in the name
        string = f'{name}: {val:.2e}'
        strings.append(string)
    if len(loss_collection) > 1:
        strings.append(f'total: {total_loss:.2e}')
    return ' + '.join(strings)

def wrap_model(data_loader, model, discr_model, img_loss, optimizer, discr_optimizer, opts):
    # create global step
    global_step = tf.train.create_global_step()
    # extract the data loader variables
    itr_next, training_init, val_init = data_loader
    # pack some necessary ones together
    data_syms = (training_init, val_init)
    # load the data batches
    input_hr_batch, input_lr_batch = itr_next
    # extra for naming
    input_lr_batch = tf.identity(input_lr_batch, name='input_image')
    # forward the lr batch through the model
    with tf.variable_scope(OPTS.MODEL_SCOPE):
        output_hr_batch = model(input_lr_batch)
    # another extra for naming
    output_hr_batch = tf.identity(output_hr_batch, name='output_image')
    # initialize discriminator variables as None
    discr_syms = None
    # forward through discriminator as well, pass as None if none
    if discr_model:
        # pass the real image through the discriminator
        with tf.variable_scope(OPTS.DISCR_SCOPE):
            logits_real = discr_model(input_hr_batch)
        # pass the fake image through the discriminator, reusing weights
        with tf.variable_scope(OPTS.DISCR_SCOPE, reuse=True):
            logits_fake = discr_model(output_hr_batch)
        # create the losses through the input & output HR images
        model_loss, discr_loss = create_loss_layer(input_hr_batch, output_hr_batch, img_loss, 
                                                   logits_real, logits_fake)
        # add the discr losses to summaries
        if OPTS.TRAIN['show_detailed_losses']:
            summarize_collection(tf.get_collection(OPTS.DISCR_LOSSES))
        tf.summary.scalar('discriminator-loss', discr_loss)
        # optimize the loss for the discr model
        # only optimize discr variables!!!
        discr_vars = tf.get_collection(tf.GraphKeys.GLOBAL_VARIABLES,
                                       scope=OPTS.DISCR_SCOPE)
        with tf.control_dependencies(tf.get_collection(OPTS.DISCR_LOSSES)):
            discr_train_op = discr_optimizer.minimize(
                discr_loss, 
                global_step=global_step, 
                var_list=discr_vars)
        # pack the discr variables together
        discr_syms = (discr_loss, discr_train_op)
    else:
        model_loss = create_loss_layer(input_hr_batch, output_hr_batch, img_loss)
    # add summaries for interesting variables
    if OPTS.TRAIN['show_detailed_losses']:
        summarize_collection(tf.get_collection(OPTS.MODEL_LOSSES))
    tf.summary.scalar('loss', model_loss) # summary for the loss
    tf.summary.image('lr_batch', input_lr_batch) # summary for the input LR
    tf.summary.image('hr_batch', input_hr_batch) # summary for the input HR
    tf.summary.image('output_batch', output_hr_batch) # summary for the output HR batch
    # also summarize trained weights as histograms
    if opts['summarize_weights']:
        for var in tf.trainable_variables():
            tf.summary.histogram(f'{var.name}', var)
    # merge summaries
    merged_summary = tf.summary.merge_all()
    # insert control dependencies and optimize the loss
    # only optimize model variables!!!
    model_vars = tf.get_collection(tf.GraphKeys.GLOBAL_VARIABLES,
                                   scope=OPTS.MODEL_SCOPE)
    with tf.control_dependencies(tf.get_collection(OPTS.MODEL_LOSSES)):
        train_op = optimizer.minimize(model_loss, global_step=global_step, var_list=model_vars)
    # pack the model variables together
    model_syms = (input_lr_batch, output_hr_batch, model_loss, train_op)
    # return some useful variables
    return model_syms, discr_syms, data_syms, merged_summary


def train(model_vars, num_epochs, scheduler, opts):
    '''
    Train the given model on the given dataset.
    '''
    # unpack the model variables
    model_syms, discr_syms, data_syms, merged_summary = model_vars
    input_lr_sym, output_hr_sym, model_loss_sym, train_op = model_syms
    train_init, val_init = data_syms
    # show each loss separately if required
    if opts['show_detailed_losses']:
        model_loss_syms = tf.get_collection(OPTS.MODEL_LOSSES)
    else:
        model_loss_syms = (model_loss_sym,)
    # also unpack discr vars if they exist
    discr_exists = discr_syms is not None
    if discr_exists:
        discr_loss_sym, discr_train_op = discr_syms
        if opts['show_detailed_losses']:
            discr_loss_syms = tf.get_collection(OPTS.DISCR_LOSSES)
        else:
            discr_loss_syms = (discr_loss_sym,)
    # create a saver & global step
    saver = tf.train.Saver()
    global_step = tf.train.get_global_step()
    # start a session
    with tf.Session() as sess:
        # check for the existence of the checkpoint directory
        if tf.gfile.Exists(opts['checkpoint_dir']):
            # find the last checkpoint file
            last_ckpt_path = tf.train.latest_checkpoint(opts['checkpoint_dir'])
            # try to load the model from there, error if it fails
            try:
                saver.restore(sess, last_ckpt_path)
            except:
                print(f'Failed to load from directory {opts["checkpoint_dir"]}.')
                print(f'Some required files must have been deleted or corrupted.')
                print(f'Please manually delete or restore directory {opts["checkpoint_dir"]}.')
                exit(1)
        else:
            # create the checkpoint directory if it does not exist
            tf.gfile.MkDir(opts['checkpoint_dir'])
            # since we are not reloading, initialize global variables
            sess.run(tf.global_variables_initializer())
        try:
            # create summary file writers
            train_writer = tf.summary.FileWriter(os.path.join(opts['log_dir'], 'training'), 
                                                             sess.graph)
            val_writer = tf.summary.FileWriter(os.path.join(opts['log_dir'], 'validation'), 
                                                            sess.graph)
            with train_writer, val_writer:
                # iterate over the training dataset num_epochs times
                for _ in range(num_epochs):
                    try:
                        # run the training initializer
                        sess.run(train_init)
                        # end of training is signaled by a tf.errors.OutOfRangeError,
                        # so we need a try-except structure
                        while True:
                            # if it is the discriminator's turn, train the discriminator
                            if discr_exists and scheduler.train_discriminator():
                                _, step, *discr_losses = sess.run(
                                    [discr_train_op, global_step, *discr_loss_syms])
                                # mark the network and calc. loss
                                loss_string = format_loss(discr_loss_syms, discr_losses)
                                network = 'discriminator'
                            else:
                                # run an iteration, get the loss and step to print them out
                                # also run on the train op to force gradient backprop
                                _, step, *model_losses = sess.run(
                                    [train_op, global_step, *model_loss_syms])
                                loss_string = format_loss(model_loss_syms, model_losses)
                                network = 'generator'

                            # print step & loss, if required by the step
                            if step % opts['print_every'] == 0:
                                print(f'Step {step}: {network} -> {loss_string}')

                            # save a checkpoint, if required by the step
                            if step % opts['save_every'] == 0:
                                saver.save(sess, opts['checkpoint_file'], global_step=global_step)
                            # log results, if required by the steps
                            if step % opts['log_every'] == 0:
                                # rerun to get a merged summary, without touching other ops
                                # TODO: fix this summary mess!
                                summary = sess.run(merged_summary)
                                # write the summary
                                train_writer.add_summary(summary, step)
                    # wait for the dataset to be exhausted (tf.errors.OutOfRangeError)
                    except tf.errors.OutOfRangeError:
                        # now, record the validation error
                        print('End of epoch reached. Calculating validation error...')
                        try:
                            # run the validation initializer
                            sess.run(val_init)
                            # run over the whole set again, calculating losses and counting
                            model_losses = []
                            vc = count(0)
                            while True:
                                print(f'\rProcessing validation batch {next(vc)}...', end='')
                                # calculate the loss of a batch
                                loss = sess.run(model_loss_sym)
                                # append it to losses
                                model_losses.append(loss)
                        # validation dataset is exhausted
                        except tf.errors.OutOfRangeError: 
                            # calculate the loss with numpy, print it
                            avg_loss = np.mean(np.array(model_losses))
                            print(f'Average validation loss of sr-model: {avg_loss:.2e}')
                            # record it by manually creating a Summary protobuf
                            val_loss_summary = tf.Summary(value=[
                                tf.Summary.Value(tag='loss', simple_value=avg_loss)])
                            val_writer.add_summary(val_loss_summary, step)
        # fully save the model (different from checkpoints), either
        # when the training ends, or user presses Ctrl+C
        except KeyboardInterrupt:
            pass
        print('Training done. Saving model...')
        tf.saved_model.simple_save(
            sess,
            opts['model_dir'],
            inputs={'input_image': input_lr_sym},
            outputs={'output_image': output_hr_sym})


def main():
    ###
    # set a seed, not sure if it works really well
    tf.set_random_seed(2018)
    # get the command line arguments
    args = parse_arguments()
    # create parts of the model
    data_loader = create_combined_data_loader(
        args.training_file, 
        args.validation_file,
        args.batch_size, 
        OPTS.DATA_LOADER)
    model = srcnn_x2_weak
    discr_model = srgan_discriminator
    scheduler = SingleScheduler(True) # GoodfellowScheduler(1)
    img_loss = mse_loss
    optimizer = tf.train.RMSPropOptimizer(args.learning_rate)
    discr_optimizer = tf.train.RMSPropOptimizer(args.discr_learning_rate)
    # combine the parts to create the full model
    model_vars = wrap_model(data_loader, model, discr_model, img_loss, optimizer, discr_optimizer, OPTS.MODEL)    
    # do the training
    train(model_vars, args.num_epochs, scheduler, OPTS.TRAIN)


if __name__ == '__main__':
    main()
