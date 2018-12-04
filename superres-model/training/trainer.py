from argparse import ArgumentParser

import tensorflow as tf

from losses import mse_loss_layer
from models import srcnn_x2_weak
import train_opts as OPTS


def parse_arguments():
    parser = ArgumentParser()
    parser.add_argument(
        'input_files',
        help='.tfrecord files storing png hr-lr pairs',
        nargs='+')
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


def decode_png(serialized_record):
    example = tf.parse_single_example(
        serialized_record,
        features={'hr_bytes': tf.FixedLenFeature([], tf.string),
                    'lr_bytes': tf.FixedLenFeature([], tf.string)})
    hr_img = tf.image.decode_png(example['hr_bytes'])
    lr_img = tf.image.decode_png(example['lr_bytes'])
    hr_img.set_shape([None, None, 3])
    lr_img.set_shape([None, None, 3])
    return hr_img, lr_img


def normalize_pair(hr_img, lr_img):
    hr_norm = tf.cast(hr_img, tf.float32) * (1.0 / 255.0) - 0.5
    lr_norm = tf.cast(lr_img, tf.float32) * (1.0 / 255.0) - 0.5
    return hr_norm, lr_norm


def create_data_loader(input_files, batch_size, num_epochs, opts):
    dataset = tf.data.TFRecordDataset(input_files)
    dataset = dataset.map(decode_png)
    dataset = dataset.map(normalize_pair)
    dataset = dataset.shuffle(opts['shuffle_multiplier'] * batch_size)
    dataset = dataset.repeat(num_epochs)
    dataset = dataset.batch(batch_size)
    dataset = dataset.prefetch(opts['prefetch_size'])
    itr = dataset.make_one_shot_iterator()
    return lambda: itr.get_next()


def wrap_model(data_loader, model, loss, optimizer, opts):
    # create global step
    global_step = tf.train.create_global_step()
    # load the data batches
    input_hr_batch, input_lr_batch = data_loader()
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
    return input_lr_batch, output_hr_batch, losses, train_op, merged_summary


def train(model_vars, opts):
    '''
    Train the given model on the given dataset.
    '''
    input_lr_sym, output_hr_sym, loss_sym, train_op, merged_summary = model_vars
    saver = tf.train.Saver()
    global_step = tf.train.get_global_step()
    with tf.Session() as sess:
        if tf.gfile.Exists(opts['checkpoint_dir']):
            last_ckpt_path = tf.train.latest_checkpoint(opts['checkpoint_dir'])
            try:
                saver.restore(sess, last_ckpt_path)
            except:
                print(f'Failed to load from directory {opts["checkpoint_dir"]}.')
                print(f'Some required files must have been deleted or corrupted.')
                print(f'Please manually delete or restore directory {opts["checkpoint_dir"]}.')
                exit(1)
        else:
            tf.gfile.MkDir(opts['checkpoint_dir'])
            sess.run(tf.global_variables_initializer())
        try:
            with tf.summary.FileWriter(opts['log_dir'], sess.graph) as summ_writer:
                while True:
                    _, loss, step = sess.run([train_op, loss_sym, global_step])

                    if step % opts['print_every'] == 0:
                        print(f'Step {step}: loss {loss:.2e}')

                    if step % opts['save_every'] == 0:
                        saver.save(sess, opts['checkpoint_file'], global_step=global_step)

                    # log results, if required by the steps
                    if step % opts['log_every'] == 0:
                        # rerun a session to get a merged summary
                        summary = sess.run(merged_summary)
                        # write the summary
                        summ_writer.add_summary(summary, step)

        except (tf.errors.OutOfRangeError, KeyboardInterrupt):
            print('Training done. Saving model...')
            tf.saved_model.simple_save(
                sess,
                opts['model_dir'],
                inputs={'input_image': input_lr_sym},
                outputs={'output_image': output_hr_sym})


def main():
    ###
    tf.set_random_seed(2018)
    args = parse_arguments()
    data_loader = create_data_loader(
        args.input_files, 
        args.batch_size, 
        args.num_epochs, 
        OPTS.DATA_LOADER)
    model = srcnn_x2_weak
    loss = mse_loss_layer
    optimizer = tf.train.AdamOptimizer(args.learning_rate)
    ### 
    model_vars = wrap_model(data_loader, model, loss, optimizer, OPTS.MODEL)
    train(model_vars, OPTS.TRAIN)

if __name__ == '__main__':
    main()
