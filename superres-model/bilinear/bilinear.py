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
    parser.add_argument(
         '--use-conv', '-c',
         help='if specified, the network will take as input a zero-interspersed image'
              ' and interpolate with a conv. layer, instead of using transposed conv.'
              ' on the base image. Added for an NNAPI compatibility test.',
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


def create_bilinear_weights(use_conv=False):
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
    if use_conv: # conv. layout is [H, W, IN_CHANNELS, OUT_CHANNELS]
        hwcf = np.transpose(fchw, [2, 3, 1, 0])
        return hwcf
    else: # transposed_conv. layout is [H, W, OUT_CHANNELS, IN_CHANNELS]
        hwfc = np.transpose(fchw, [2, 3, 0, 1])
        return hwfc


def build_bilinear_interpolator(use_conv=False):
    # reset the default graph
    # tf.reset_default_graph()
    # define the input as a placeholder
    x = tf.placeholder(tf.float32, [None, None, None, 3], name='input_image')
    # create a kernel initializer from the weights
    linear_interpolation_initializer = tf.constant_initializer(
        value=create_bilinear_weights(use_conv),
        dtype=tf.float32,
        verify_shape=True)
    if use_conv:
        # define as taking the same size 0 interspersed image, simply apply conv
        i = tf.layers.conv2d(
            inputs=x,
            filters=3,
            kernel_size=3,
            strides=1,
            padding='same',
            kernel_initializer=linear_interpolation_initializer,
            name='interpolation_layer')
    else:
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


def zero_expand_image(image):
    # expand the 3-channel image by a factor of 2, interspersing zeros between empty spaces
    # example: 
    # [1, 2]     [1, 0, 2, 0]
    # [3, 4] --> [0, 0, 0, 0]
    #            [3, 0, 4, 0]
    #            [0, 0, 0, 0]
    h, w, c  = image.shape
    expanded_image = np.zeros((2*h, 2*w, c))
    expanded_image[::2, ::2, :] = image
    return expanded_image

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
            xsym, ysym = build_bilinear_interpolator(args.use_conv)
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
        # select an image expander if use_conv is specified
        expander = zero_expand_image if args.use_conv else lambda x: x
        # go through each image path
        for impath in args.image_paths:
            # read the BGR image
            raw_image = cv2.imread(impath, cv2.IMREAD_COLOR)
            # if use_conv is specified, expand the image
            expanded_image = expander(raw_image)
            # convert the image to float and add a 4th batch axis
            image = np.expand_dims(expanded_image.astype('float32'), axis=0)
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
