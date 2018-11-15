from multiprocessing import Process, Queue
from argparse import ArgumentParser
from sys import stderr
from glob import glob

from tqdm import tqdm
import tensorflow as tf
import numpy as np
import cv2


def parse_arguments():
    parser = ArgumentParser(description='A script to convert images to' 
        ' patches and save them in tfrecords.')
    parser.add_argument(
        'hr_image_glob',
        help='glob expression for paths to the high-res input images, use single quotes')
    parser.add_argument(
        'lr_image_glob',
        help='glob expression for paths to the low-res input images, use single quotes')
    parser.add_argument(
        '--outfile-name', '-o',
        help='name of the output file where the image patch pairs will be saved.'
             'the default basename is data.tfrecord',
        default='data.tfrecord')
    parser.add_argument(
        '--patch-size', '-p',
        help='the patch size to use when dividing the images, 256x256 by default',
        default='256x256')
    parser.add_argument(
        '--downsampling-factor', '-d',
        help='the factor by which input images are downsampled, used to determine patch size'
             ' for the low-res images. 2 by default.',
        type=int,
        default=2)
    parser.add_argument(
        '--njobs', '-n',
        help='the number of jobs to run in parallel. Defaults to 1.',
        type=int,
        default=1)
    arguments = parser.parse_args()
    try:
        arguments.patch_size = tuple(map(int, arguments.patch_size.strip().split('x')))
        if len(arguments.patch_size) != 2:
            raise ValueError('Patch size should only contain 2 values')
        arguments.patch_size = np.array(arguments.patch_size)
    except:
        print('Could not parse input patch size. Format should be nxm.', file=stderr)
        exit(1)
    return arguments


def divide_image(img, patch_shape, discard_extra=True):
    '''
    A function that splits the image into equally sized patchess of (patch_shape[0], 
    patch_shape[1]). If the image cannot be divided equally to the given grid size, 
    the remaining part can either be discarded, or the last patch in each row/column
    can be created by aligning to the end. Note that this will cause an overlap.
    Arguments:
    - img: A 3D input image in the form of a numpy.ndarray
    - patch_shape: A 2-tuple of the form (patch_height, patch_width)
    - discard_extra: If True, the part of the image that does not fit will be discarded.
        Otherwise, extra patches aligned to the right and bottom will be created. Note
        that these aligned patches have to overlap with existing patches to make the size
        uniform. Currently not implemented.
    Returns:
    - patch_array: a 4D numpy.ndarray of patches having the same size.
    '''
    if not discard_extra:
        raise NotImplementedError
    img_height, img_width = img.shape[0], img.shape[1]
    patch_height, patch_width = patch_shape
    patches = []
    for i in range(0, img_height-patch_height+1, patch_height):
        for j in range(0, img_width-patch_width+1, patch_width):
            patch = img[i:i+patch_height, j:j+patch_width]
            patches.append(patch)
    patch_array = np.stack(patches, axis=0)
    return patch_array


def infer_downsampling_factor(sample_hr, sample_lr):
    ''' 
    Infers the downsampling factor between two images by comparing their shapes. The
    functionality is very basic for now and would only work for exact downsampling.
    '''
    h_ratio = round(sample_hr.shape[0] / sample_lr.shape[0])
    w_ratio = round(sample_hr.shape[1] / sample_lr.shape[1])
    assert h_ratio == w_ratio, \
        'Error inferring downsampling factor, width & height estimates do not match'
    ds_factor = h_ratio
    return ds_factor

# multiprocessing queues
input_queue = Queue()
output_queue = Queue()
# shared runtime constants 
ds_factor = None
hr_patch_size = None
lr_patch_size = None

def process_image_pair(hr_image_path, lr_image_path):
    # read both images as arrays 
    hr_img = cv2.imread(hr_image_path, cv2.IMREAD_COLOR)
    lr_img = cv2.imread(lr_image_path, cv2.IMREAD_COLOR)
    # infer the downsampling factor for checking
    assert infer_downsampling_factor(hr_img, lr_img) == ds_factor, \
        'Image pair downsampling ratio does not match previous images'
    # batch the images
    hr_batches = divide_image(hr_img, hr_patch_size)
    lr_batches = divide_image(lr_img, lr_patch_size)
    assert hr_batches.shape[0] == lr_batches.shape[0], \
        'Number of image patches do not match, cannot pair'
    # create a tf.train.Example for each patch
    serialized_pairs = []
    for hr_patch, lr_patch in zip(hr_batches, lr_batches):
        # TODO: think about whether this really is the best way to store the info
        # encode the patches a png
        r1, hr_png = cv2.imencode('.png', hr_patch)
        r2, lr_png = cv2.imencode('.png', lr_patch)
        assert r1 and r2, 'png conversion of patches failed'
        # serialize and add the png pair
        serialized_pairs.append((hr_png.tostring(), lr_png.tostring()))
    # return all the png pairs
    return serialized_pairs


def image_pair_processor(ds_factor):
    # keep image path pairs and processing them forever
    while True:
        hr_path, lr_path = input_queue.get()
        serialized_pairs = process_image_pair(hr_path, lr_path)
        output_queue.put(serialized_pairs)


def fork_workers(args):
    # expand the glob expressions and check that the number of files match
    hr_image_paths = sorted(glob(args.hr_image_glob))
    lr_image_paths = sorted(glob(args.lr_image_glob))
    if len(hr_image_paths) != len(lr_image_paths):
        raise ValueError('The number of HR and LR images should be the same.')
    # read an image pair to infer the downsampling factor, and set the global
    # shared constants
    # TODO: check details for 3x downsampling, not important for now
    sample_hr = cv2.imread(hr_image_paths[0], cv2.IMREAD_COLOR)
    sample_lr = cv2.imread(lr_image_paths[0], cv2.IMREAD_COLOR)
    global ds_factor
    ds_factor = infer_downsampling_factor(sample_hr, sample_lr)
    print(f'Inferred downsampling factor as {ds_factor}.')
    global hr_patch_size
    hr_patch_size = args.patch_size
    global lr_patch_size
    lr_patch_size = hr_patch_size // ds_factor
    # put all path pairs in the input queue
    for hr_path, lr_path in zip(hr_image_paths, lr_image_paths):
        input_queue.put((hr_path, lr_path))
    # fork n image processors
    processes = [Process(target=image_pair_processor, args=(ds_factor,), daemon=True) \
        for _ in range(args.njobs)]
    for process in processes:
        process.start()
    # no need to join since they are daemonic


def write_records(args):
    # here, I take a shortcut since we know the number of image
    # pairs beforehand. This saves us from having to use more
    # synchronization mechanisms
    nimages = len(glob(args.hr_image_glob))
    writer_opts = tf.python_io.TFRecordOptions(tf.python_io.TFRecordCompressionType.NONE)
    with tf.python_io.TFRecordWriter(args.outfile_name, writer_opts) as record_writer:
        for _ in tqdm(range(nimages)):
            serialized_pairs = output_queue.get()
            for hr_png, lr_png in serialized_pairs:
                sample = tf.train.Example(
                    features=tf.train.Features(
                        feature={
                            'hr_bytes': tf.train.Feature(
                                bytes_list=tf.train.BytesList(value=[hr_png])),
                            'lr_bytes': tf.train.Feature(
                                bytes_list=tf.train.BytesList(value=[lr_png]))}))
                # serialize and write the example
                serialized_sample = sample.SerializeToString()
                record_writer.write(serialized_sample)


def main():
    # parse the command line arguments
    args = parse_arguments()
    # fork the workers
    workers = fork_workers(args)
    # write records using worker outputs
    write_records(args)
    # exit, hopefully the children will be terminated since they are daemonic

if __name__ == '__main__':
    main()
