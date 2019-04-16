import numpy as np

import idx


def center_images(data):
    # normalize and center the data
    return (data.astype('float32') / 255.0 - 0.5) * 2

def get_set(data_path, label_path, label):
    images = center_images(
        np.expand_dims(
            idx.readfile(data_path, 3),
            axis=-1))
    labels = np.expand_dims(
        idx.readfile(label_path, 1),
        axis=-1)
    print(images.shape)
    print(labels.shape)
    if label:
        indices = np.squeeze(labels == label)
        images = images[indices, :, :, :]
        labels = labels[indices, :]
    print(images.shape)
    print(labels.shape)
    return images, labels

def get_training_set(label=None):
    return get_set('dataset/train-images-idx3-ubyte', 'dataset/train-labels-idx1-ubyte', label) 

def get_test_set(label=None):
    return get_set('dataset/t10k-images-idx3-ubyte', 'dataset/t10k-labels-idx1-ubyte', label)

def get_noise_batch(batch_size, latent_size):
    return np.random.uniform(-1, 1, size=(batch_size, latent_size)) # roughly between -1 and 1

def get_discriminator_training_set(batch_size, label=None):
    # get the training set and its size
    training_images, _ = get_training_set(label)
    N = training_images.shape[0]
    # interleave them in a single set along with labels
    # I could not come up with a simple & powerful way with no for loop :(
    hb_size = batch_size // 2
    mixed_training_images = np.empty((2*N, 28, 28, 1), dtype='float32')
    mixed_training_labels = np.empty(2*N, dtype='uint8')
    for i in range(0, 2*N, batch_size):
        j = i // 2
        # half real mnist, half noise
        mixed_training_images[i:i+hb_size, :, :, :] = training_images[j:j+hb_size]
        mixed_training_images[i+hb_size:i+batch_size, :, :, :] = get_noise_batch(hb_size)
        mixed_training_labels[i:i+hb_size] = np.ones(hb_size, dtype='uint8')
        mixed_training_labels[i+hb_size:i+batch_size] = np.zeros(hb_size, dtype='uint8')
    # return the new set
    return mixed_training_images, mixed_training_labels

def get_discriminator_test_set(label=None):
    # get the test set
    test_images, _ = get_test_set(label)
    N = test_images.shape[0]
    # create test labels
    test_labels = np.ones(N, dtype='uint8')
    # create noise images & labels
    noise_images = np.random.rand(N, 28, 28, 1)
    noise_labels = np.zeros(N, dtype='uint8')
    # no need to mix since we are not training, concat instead
    aug_test_images = np.concatenate((test_images, noise_images))
    aug_test_labels = np.concatenate((test_labels, noise_labels))
    return aug_test_images, aug_test_labels
