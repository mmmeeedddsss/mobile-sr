from argparse import ArgumentParser
from time import time

from tqdm import tqdm
import tensorflow as tf
import matplotlib.pyplot as plt


def parse_arguments():
    parser = ArgumentParser()
    parser.add_argument(
        'input_files',
        help='input .tfrecord files',
        nargs='+')
    parser.add_argument(
        '--read', '-r',
        help='if specified, the image pairs will not be shown, only the data will be read for'
             ' testing.',
        action='store_true')
    arguments = parser.parse_args()
    return arguments


def decode_png(serialized_record):
    example = tf.parse_single_example(
        serialized_record,
        features={'hr_bytes': tf.FixedLenFeature([], tf.string),
                  'lr_bytes': tf.FixedLenFeature([], tf.string)})
    hr_img = tf.image.decode_png(example['hr_bytes'])
    lr_img = tf.image.decode_png(example['lr_bytes'])
    return hr_img, lr_img


def set_up_dataset(input_files):
    # set up the dataset
    dataset = tf.data.TFRecordDataset(input_files)
    dataset = dataset.map(decode_png)
    dataset = dataset.prefetch(32)
    itr = dataset.make_one_shot_iterator()
    hr_sym, lr_sym = itr.get_next()
    return hr_sym, lr_sym


def read_dataset(args):
    hr_sym, lr_sym = set_up_dataset(args.input_files)
    try:
        with tf.Session() as sess, tqdm(desc='Image pairs read:') as progress_tracker:
            while True:
                hr_img, lr_img = sess.run((hr_sym, lr_sym)) # get the imgs
                progress_tracker.update()
    except tf.errors.OutOfRangeError:
        print('\nFinished reading dataset.')


def show_dataset(args):
    hr_sym, lr_sym = set_up_dataset(args.input_files)
    # set up a figure for showing images over
    fig = plt.figure()
    hr_ax = plt.subplot(1, 2, 1)
    lr_ax = plt.subplot(1, 2, 2)
    try:
        # start the tf session
        with tf.Session() as sess:
            # create an iteration function and connect it to a keypress event
            def advance(event):
                hr_img, lr_img = sess.run((hr_sym, lr_sym)) # get the imgs
                hr_ax.imshow(hr_img) # show hr on the left
                lr_ax.imshow(lr_img) # show lr on the right
                fig.canvas.draw_idle() # redraw the figure
            advance(None) # advance to draw the first patch
            fig.canvas.mpl_connect('key_press_event', advance) # connect 
            plt.show() # create the figure window
    except tf.errors.OutOfRangeError:
        print('All images shown.')


if __name__ == '__main__':
    args = parse_arguments()
    if args.read:
        read_dataset(args)
    else:
        show_dataset(args)
