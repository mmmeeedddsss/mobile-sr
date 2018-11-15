from argparse import ArgumentParser
from sys import stderr
from multiprocessing import Pool, Value

from tqdm import tqdm
import numpy as np
import cv2
import matplotlib.pyplot as plt


def parse_arguments():
    parser = ArgumentParser(description='A script to convert a list of images to' 
        ' 4D numpy ndarrays and save it.')
    parser.add_argument(
        'image_paths',
        help='paths to the input images',
        nargs='+')
    parser.add_argument(
        '--max-file-size', '-m',
        help='the approximate maximum output file size. Should be less than half the computer\'s'
             ' memory. If the dataset exceeds this size, multiple output files will be produced.'
             ' 1073741824 (1 GB) by default.',
        type=int,
        default=1073741824)
    parser.add_argument(
        '--outfile-base', '-o',
        help='basename of the output file where the images will be saved.'
             'output data files will be saved as basename0.npy, basename1.npy etc.'
             'the default basename is data',
        default='data')
    parser.add_argument(
        '--patch-size', '-p',
        help='the patch size to use when dividing the images, 256x256 by default',
        default='256x256')
    parser.add_argument(
        '--njobs', '-n',
        help='the number of processes to run in parallel. Note that this will cause more'
             ' than one file to have a size less than max_file_size. Defaults to 1.'
             ' Does not bring much improvement due to the task being IO bound.',
        type=int,
        default=1)
    arguments = parser.parse_args()
    try:
        arguments.patch_size = tuple(map(int, arguments.patch_size.strip().split('x')))
        if len(arguments.patch_size) != 2:
            raise ValueError('Patch size should only contain 2 values')
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


def process(args):
    # get the inputs
    i, image_paths, patch_size, basename, max_file_size = args
    # set the bytes and batches to zero
    batch_bytes = 0
    batches = []
    # for each image
    for image_path in tqdm(image_paths, position=i, leave=False):
        # read the image from disk and convert to RGB
        img = cv2.imread(image_path, cv2.IMREAD_COLOR)
        cv2.cvtColor(img, cv2.COLOR_BGR2RGB, dst=img)
        # divide the image into multiple patches
        batch = divide_image(img, patch_size)
        # increment the batch byte counter
        batch_bytes += batch.nbytes
        # add the batch
        batches.append(batch)
        # if the max file size is exceeded
        if batch_bytes >= max_file_size:
            # lock the file_id counter, get it and increment it
            with file_id.get_lock():
                fid = file_id.value
                file_id.value += 1
            # create a large 4D array by concatenating the batches
            data_file = np.concatenate(batches, axis=0)
            # save the file to disk
            np.save(f'{basename}{fid}.npy', data_file)
            # reset the counters
            batch_bytes = 0
            batches = []
    # if there are any batches left, save them to disk like before
    if batches:
        with file_id.get_lock():
            fid = file_id.value
            file_id.value += 1
        data_file = np.concatenate(batches, axis=0)
        np.save(f'{basename}{fid}.npy', data_file)


def main():
    # parse the command line arguments
    args = parse_arguments()
    # divide the image paths into njobs groups
    chunk_size = len(args.image_paths) // args.njobs
    image_path_groups = [args.image_paths[i:i+chunk_size] \
        for i in range(0, len(args.image_paths), chunk_size)]
    # process everything in parallel
    args_list = [(i, group, args.patch_size, args.outfile_base, args.max_file_size) \
        for i, group in enumerate(image_path_groups)]
    with Pool(args.njobs) as pool:
        pool.map(process, args_list)


if __name__ == '__main__':
    # create a shared file_id wrapper for multiprocesses
    file_id = Value('i', 0)
    # do it
    main()
