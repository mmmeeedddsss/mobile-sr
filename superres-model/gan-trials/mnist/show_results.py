import os
from argparse import ArgumentParser
from subprocess import run
from sys import argv

import numpy as np
import matplotlib.pyplot as plt
from PIL import Image


IMGS_PER_ROW = 4

def parse_arguments():
    parser = ArgumentParser()
    parser.add_argument(
        'image_dir',
        help='path to the directory where gan.py output images are stored')
    parser.add_argument(
        'loss_file',
        help='path to the file where gan.py output losses are stored')
    parser.add_argument(
        '--gifit', '-g',
        help='if specified, a GIF will be created from batch outputs',
        action='store_true')
    parser.add_argument(
        '--delay', '-d',
        help='delay between each frame in the GIF, in centiseconds. Defaults to 100.',
        type=int,
        default=100)
    parser.add_argument(
        '--out-path', '-o',
        help='path to the output GIF file. Defaults to result.gif',
        default='result.gif')
    args = parser.parse_args()
    return args

def determine_batch_size(imdir):
    """ open a batch directory, count the number of images and return it """
    count = 0
    size = 0
    for epochdir in os.scandir(imdir):
        count += 1
    for imfile in os.scandir(epochdir):
        size += 1
    return count, size

def show_losses(loss_file):
    lf = np.load(loss_file)
    N = lf['discr_loss'].size
    plt.figure()
    plt.plot(np.arange(0, N), lf['discr_loss'])
    plt.plot(np.arange(0, N), lf['adv_loss'])
    plt.grid()
    plt.title('Discriminator and Adversarial Losses')
    plt.legend(['Discriminator Loss', 'Adversarial Loss'])

if __name__ == '__main__':
    args = parse_arguments()

    # show the losses on a plot
    show_losses(args.loss_file)
    plt.show()

    # get statistics for the dataset
    epoch_count, batch_size = determine_batch_size(args.image_dir)
    print(f'Epoch count is {epoch_count}, size is {batch_size}')
    ncols = IMGS_PER_ROW
    nrows = int(np.ceil(batch_size / ncols))

    # create plotting variables
    fig = plt.figure(figsize=(8, 8))
    axes = [fig.add_subplot(nrows, ncols, i+1) for i in range(batch_size)]
    keyf = lambda x: int(x.split('/')[-1])
    epoch_dirs = sorted([epochdir.path for epochdir in os.scandir(args.image_dir)], key=keyf)

    epoch_index = 0
    imshow_objs = []
    def plot_batch(event): # callback function for switching between batches
        # move forward or backward in batches using left & right arrow keys
        global epoch_index
        if event:
            if event.key == 'right':
                epoch_index = (epoch_index + 1) % epoch_count
            elif event.key == 'left':
                epoch_index = (epoch_index - 1) % epoch_count
        # find the current batch directory
        global imshow_objs
        epochdir = epoch_dirs[epoch_index]
        for i, (ax, imfile) in enumerate(zip(axes, os.scandir(epochdir))): # for each image
            img_obj = Image.open(imfile.path) # open it 
            img = np.array(img_obj) # convert to ndarray
            ax.set_xticklabels([]) # remove plot ticks
            ax.set_yticklabels([])
            if len(imshow_objs) == batch_size: # use set data, imshow already done
                imshow_objs[i].set_data(img)
            else: # first call, use imshow
                obj = ax.imshow(img, cmap='gray', aspect='equal') # show it
                imshow_objs.append(obj)
        fig.subplots_adjust(hspace=0, wspace=0) # adjust for no space
        fig.suptitle(f'Generator after epoch {epoch_index+1}')
        plt.draw() 

    if args.gifit: # create a GIF before showing
        print('Creating GIF...')
        # go through each epoch, plot it and create a PNG, and store its path
        impaths = []
        while epoch_index < epoch_count:
            impath = f'{epoch_index}.png'
            plot_batch(None)
            plt.savefig(impath)
            impaths.append(impath)
            epoch_index += 1
        # create a GIF from the PNGs with imagemagick
        run(['convert', '-delay', f'{args.delay}', '-loop', '0', *impaths, args.out_path])
        # remove all the pngs
        run(['rm', *impaths])
        print('Done!')
        epoch_index = 0

    # then, connect the callback and draw generator outputs
    fig.canvas.mpl_connect('key_press_event', plot_batch) # connect the callback
    plot_batch(None) # plot the zeroth batch
    plt.show()

