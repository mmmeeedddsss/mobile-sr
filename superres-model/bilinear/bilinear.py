from sys import stderr
from argparse import ArgumentParser

import cv2
import numpy as np
import tensorflow as tf


def parse_arguments():
    # define, read and verify the command line arguments
    parser = ArgumentParser()
    parser.add_argument(
        'image_paths',
        help='paths to input images',
        nargs='*')
    parser.add_argument(
        '--extension', '-e',
        help='name extension for output images',
        default='_interp')
    parser.add_argument(
        '--model-path', '-p',
        help='if specified, save model to the specified path')
    parser.add_argument(
        '--load',
        help='if specified, load the model from the specified path instead of creating it',
        action='store_true')
    args = parser.parse_args()
    if args.load and not args.model_path:
        print('error: load can only specified along with a model path', file=stderr)
        exit(1)
    return args


def extend_filename(file_path, extra):
    # add an extra to the file name, such as _interp
    left, extension = file_path.rsplit('.')
    return left + extra + '.' + extension


def create_bilinear_weights():
    # create weights for the transposed conv2d layer
    # that correspond to bilinear interpolation
    interp3 = [[.25, 0.5, .25],
               [.50, 1.0, .50],
               [.25, 0.5, .25]]
    zeros3 = [[0.0, 0.0, 0.0],
              [0.0, 0.0, 0.0],
              [0.0, 0.0, 0.0]]
    fchw = np.array([[interp3, zeros3, zeros3],
                     [zeros3, interp3, zeros3],
                     [zeros3, zeros3, interp3]])
    hwfc = np.transpose(fchw, [2, 3, 0, 1])
    return hwfc


def build_bilinear_interpolator():
    # reset the default graph
    # tf.reset_default_graph()
    # define the input as a placeholder
    x = tf.placeholder(tf.float32, [None, None, None, 3], name='input_image')
    # create a kernel initializer from the weights
    linear_interpolation_initializer = tf.constant_initializer(
        value=create_bilinear_weights(),
        dtype=tf.float32,
        verify_shape=True)
    # define the transpose conv2d layer which does bilinear interpolation
    i = tf.layers.conv2d_transpose(
        inputs=x,
        filters=3,
        kernel_size=3,
        strides=2,
        padding='same',
        kernel_initializer=linear_interpolation_initializer,
        name='interpolation_layer')
    # define the output separately for easy loading
    y = tf.identity(i, name='output_image')
    # return the input and output symbolic variables
    return x, y


def interpolate_images(args):
    # create a session
    with tf.Session() as sess:
        # if requested, load the model instead of recreating it
        if args.load:
            tf.saved_model.loader.load(
                sess,
                [tf.saved_model.tag_constants.SERVING],
                args.model_path)
            # get the symbolic variables through the default graph
            dg = tf.get_default_graph()
            xsym = dg.get_tensor_by_name('input_image:0')
            ysym = dg.get_tensor_by_name('output_image:0')
        # else, create the graph and unpack the symbolic variables
        else:
            xsym, ysym = build_bilinear_interpolator()
            # run the global variable initializer
            sess.run(tf.global_variables_initializer())
            # export the model
            if args.model_path:
                tf.saved_model.simple_save(
                    sess,
                    args.model_path,
                    inputs={'input_image': xsym},
                    outputs={'output_image': ysym})
        # finalize the graph to make sure we do not add any more ops
        sess.graph.finalize()
        # go through each image path
        for impath in args.image_paths:
            # read the BGR image
            raw_image = cv2.imread(impath, cv2.IMREAD_COLOR)
            # convert the image to float and add a 4th batch axis
            image = np.expand_dims(raw_image.astype('float32'), axis=0)
            # create a feed_dict, assigning the image as the input
            feed_dict = {xsym: image}
            # run a session with the feed dict, getting 'y' as output
            interp_image = sess.run(ysym, feed_dict=feed_dict)
            # remove the batch axis and cast back to 8-bits
            raw_interp_image = np.squeeze(interp_image).astype('uint8')
            # save the image with an extended name
            cv2.imwrite(extend_filename(impath, args.extension), raw_interp_image)


if __name__ == '__main__':
    arguments = parse_arguments()
    interpolate_images(arguments)
