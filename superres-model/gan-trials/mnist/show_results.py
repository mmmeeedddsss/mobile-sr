import os
from subprocess import run
from sys import argv

import numpy as np
import matplotlib.pyplot as plt
from PIL import Image


IMGS_PER_ROW = 4
GIF_DELAY = 100 # in cs
GIF_PATH = 'result.gif'

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
    if len(argv) == 2:
        _, imdir = argv
        lossfile = None
    elif len(argv) == 3:
        _, imdir, lossfile = argv

        # start by showing the losses 
        show_losses(lossfile)
        plt.show()
    else:
        print('Usage: python show_results.py image_directory [losses.npz]')
        print('       if losses.npz is not provided, a GIF is created from the image_directory')
        exit(1)

    # get statistics for the dataset
    epoch_count, batch_size = determine_batch_size(imdir)
    print(f'Epoch count is {epoch_count}, size is {batch_size}')
    ncols = IMGS_PER_ROW
    nrows = int(np.ceil(batch_size / ncols))

    # create plotting variables
    fig = plt.figure(figsize=(8, 8))
    axes = [fig.add_subplot(nrows, ncols, i+1) for i in range(batch_size)]
    keyf = lambda x: int(x.split('/')[-1])
    epoch_dirs = sorted([epochdir.path for epochdir in os.scandir(imdir)], key=keyf)

    epoch_index = 0
    imshow_objs = []
    def plot_batch(event): # callback function for switching between batches
        # move forward or backward in batches if the mouse wheel was used
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

    if lossfile: # show the generated images
        # then, connect the callback and draw generator outputs
        fig.canvas.mpl_connect('key_press_event', plot_batch) # connect the callback
        plot_batch(None) # plot the zeroth batch
        plt.show()
    else: # create a GIF from the generated images
        impaths = []
        while epoch_index < epoch_count:
            impath = f'{epoch_index}.png'
            plot_batch(None)
            plt.savefig(impath)
            impaths.append(impath)
            epoch_index += 1
        run(['convert', '-delay', f'{GIF_DELAY}', '-loop', '0', *impaths, GIF_PATH])
        run(['rm', *impaths])
