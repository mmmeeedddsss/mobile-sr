from argparse import ArgumentParser
from itertools import count
import os

import tensorflow as tf
import numpy as np

from data import create_combined_data_loader
from losses import mse_loss_layer
from models import srcnn_x2_weak
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


def wrap_model(data_loader, model, loss, optimizer, opts):
    # create global step
    global_step = tf.train.create_global_step()
    # extract the data loader variables
    iterator, training_initializer, validation_initializer = data_loader
    # load the data batches
    input_hr_batch, input_lr_batch = iterator 
    # extra for naming
    input_lr_batch = tf.identity(input_lr_batch, name='input_image')
    # forward the lr batch through the model, also send the hr to infer ratio
    output_hr_batch = model(input_lr_batch)
    # another extra for naming
    output_hr_batch = tf.identity(output_hr_batch, name='output_image')
    # get the losses through the input & output HR images
    losses = loss(input_hr_batch, output_hr_batch)
    # add summaries for interesting variables
    tf.summary.scalar('loss', losses) # summary for the loss
    tf.summary.image('lr_patch', input_lr_batch) # summary for the input LR
    tf.summary.image('hr_batch', input_hr_batch) # summary for the input HR
    tf.summary.image('output_batch', output_hr_batch) # summary for the output HR batch
    # also summarize trained weights as histograms
    if opts['summarize_weights']:
        for var in tf.trainable_variables():
            tf.summary.histogram(f'{var.name}', var)
    # merge summaries
    merged_summary = tf.summary.merge_all()

    # insert control dependencies and optimize the loss
    with tf.control_dependencies(tf.get_collection(tf.GraphKeys.UPDATE_OPS)):
        train_op = optimizer.minimize(losses, global_step=global_step)
    # return some useful variables
    return input_lr_batch, output_hr_batch, losses, train_op, merged_summary, training_initializer, validation_initializer


def train(model_vars, num_epochs, opts):
    '''
    Train the given model on the given dataset.
    '''
    # unpack the model variables
    (input_lr_sym, output_hr_sym, loss_sym, train_op, 
        merged_summary, train_init, val_init) = model_vars
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
                        # end of training is signaled by a tf.errors.OutOfRangeError
                        # exception, so our while loop runs forever
                        while True:
                            # run an iteration, get the loss and step to print them out
                            # also run on the train op to force gradient backprop
                            _, loss, step = sess.run([train_op, loss_sym, global_step])

                            # print step & loss, if required by the step
                            if step % opts['print_every'] == 0:
                                print(f'Step {step}: loss {loss:.2e}')

                            # save a checkpoint, if required by the step
                            if step % opts['save_every'] == 0:
                                saver.save(sess, opts['checkpoint_file'], global_step=global_step)

                            # log results, if required by the steps
                            if step % opts['log_every'] == 0:
                                # rerun to get a merged summary, without touching other ops
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
                            losses = []
                            vc = count(0)
                            while True:
                                print(f'\rProcessing validation batch {next(vc)}...', end='')
                                # calculate the loss of a batch
                                loss = sess.run(loss_sym)
                                # append it to losses
                                losses.append(loss)
                        # validation dataset is exhausted
                        except tf.errors.OutOfRangeError: 
                            # calculate the loss with numpy, print it
                            avg_loss = np.mean(np.array(losses))
                            print(f'Average validation loss: {avg_loss:.2e}')
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
    loss = mse_loss_layer
    optimizer = tf.train.AdamOptimizer(args.learning_rate)
    # combine the parts to create the full model
    model_vars = wrap_model(data_loader, model, loss, optimizer, OPTS.MODEL)
    # do the training
    train(model_vars, args.num_epochs, OPTS.TRAIN)


if __name__ == '__main__':
    main()
