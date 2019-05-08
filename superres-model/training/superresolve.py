from argparse import ArgumentParser
import os

import numpy as np
import cv2
import tensorflow as tf

def parse_arguments():
    parser = ArgumentParser()
    parser.add_argument(
        'saved_model_dir',
        help='directory where the model is saved, ideally using tf.saved_model.simple_save')
    parser.add_argument(
        'image_paths',
        help='paths to images which will be super-resolved.',
        nargs='+')
    parser.add_argument(
        '--ext-name', '-e',
        help='postfix that will be applied to superresolved images. "-sr" by default',
        default='-sr')
    parser.add_argument(
        '--output-dir', '-o',
        help='directory where the output images will be stored. "sr-images" by default.',
        default='sr-images')
    arguments = parser.parse_args()
    return arguments


def extend_filename(file_path, extra):
    # add an extra to the file name, such as _interp
    left, extension = file_path.rsplit('.', 1)
    return left + extra + '.' + extension


def load_model(sess, model_path):
    tf.saved_model.loader.load(
        sess,
        [tf.saved_model.tag_constants.SERVING],
        model_path)
    # get the symbolic variables through the default graph
    dg = tf.get_default_graph()
    input_sym = dg.get_tensor_by_name('input_image:0')
    output_sym = dg.get_tensor_by_name('output_image:0')
    return input_sym, output_sym


def bgr2rgbnb(img):
    rgb = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)
    rgbnorm = rgb.astype('float64') / 127.5 - 1.0
    batched = np.expand_dims(rgbnorm, axis=0)
    return batched


def rgbnb2bgr(img):
    rgbnorm = np.clip(np.squeeze(img), -1.0, +1.0)
    print(rgbnorm.min(), rgbnorm.max())
    rgb = ((rgbnorm + 1.0) * 127.5).astype('uint8')
    bgr = cv2.cvtColor(rgb, cv2.COLOR_RGB2BGR)
    return bgr


def main():
    args = parse_arguments()
    os.makedirs(args.output_dir, exist_ok=True)
    with tf.Session() as sess:
        input_sym, output_sym = load_model(sess, args.saved_model_dir)
        for image_path in args.image_paths:
            lr_img = cv2.imread(image_path, cv2.IMREAD_COLOR)
            lr_norm = bgr2rgbnb(lr_img)
            hr_norm = sess.run(output_sym, feed_dict={input_sym: lr_norm})
            hr_img = rgbnb2bgr(hr_norm)
            ext_path = extend_filename(image_path, args.ext_name)
            out_path = os.path.join(args.output_dir, os.path.basename(ext_path))
            cv2.imwrite(out_path, hr_img)


if __name__ == '__main__':
    main()
