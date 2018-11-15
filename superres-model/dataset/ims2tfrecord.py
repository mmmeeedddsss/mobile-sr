from argparse import ArgumentParser
from glob import glob

from tqdm import tqdm
import tensorflow as tf


def parse_arguments():
    parser = ArgumentParser()
    parser.add_argument(
        'hr_images_path',
        help='glob expression for paths to the input high-resolution images, use single quotes')
    parser.add_argument(
        'lr_images_path',
        help='glob expression for paths to the input low-resolution images, use single quotes')
    parser.add_argument(
        '--outfile', '-o',
        help='path to the output TFRecord file. data.tfrecord by default.',
        default='data.tfrecord')
    arguments = parser.parse_args()
    return arguments


def read_bytes(file_path):
    '''Read a file in binary mode and return the content as a bytes object'''
    with open(file_path, 'rb') as f:
        content = f.read()
    return content


def build_tfrecord(hr_glob, lr_glob, output_path):
    # expand the glob expressions and check that the number of files match
    hr_image_paths = sorted(glob(hr_glob))
    lr_image_paths = sorted(glob(lr_glob))
    if len(hr_image_paths) != len(lr_image_paths):
        raise ValueError('The number of HR and LR images should be the same.')
    # create a tfrecord writer and open it
    writer_opts = tf.python_io.TFRecordOptions(tf.python_io.TFRecordCompressionType.NONE)
    with tf.python_io.TFRecordWriter(output_path, writer_opts) as record_writer:
        # zip the image paths and add a progress bar on top
        # we need to turn the generator into a list to get a full progress bar
        paired_paths = list(zip(hr_image_paths, lr_image_paths))
        for hr_image_path, lr_image_path in tqdm(paired_paths):
            # read both images in raw bytes format as png
            hr_img_bytes = read_bytes(hr_image_path)
            lr_img_bytes = read_bytes(lr_image_path)
            # create a tf.train.Example
            sample = tf.train.Example(
                features=tf.train.Features(
                    feature={
                        'hr_bytes': tf.train.Feature(
                            bytes_list=tf.train.BytesList(value=[hr_img_bytes])),
                        'lr_bytes': tf.train.Feature(
                            bytes_list=tf.train.BytesList(value=[lr_img_bytes]))}))
            # serialize and write the example
            serialized_sample = sample.SerializeToString()
            record_writer.write(serialized_sample)


if __name__ == '__main__':
    args = parse_arguments()
    build_tfrecord(args.hr_images_path, args.lr_images_path, args.outfile)
