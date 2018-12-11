import tensorflow as tf


def decode_png(serialized_record):
    ''' a function for decoding a serialized record into an lr-hr image pair '''
    # use parse_single example to extract an lr-hr pair
    example = tf.parse_single_example(
        serialized_record,
        features={'hr_bytes': tf.FixedLenFeature([], tf.string),
                   'lr_bytes': tf.FixedLenFeature([], tf.string)})
    # decode both the low-res and high-res images, since they are encoded as png
    hr_img = tf.image.decode_png(example['hr_bytes'])
    lr_img = tf.image.decode_png(example['lr_bytes'])
    # some future operations require the channel shape to be set 
    hr_img.set_shape([None, None, 3])
    lr_img.set_shape([None, None, 3])
    return hr_img, lr_img


def normalize_pair(hr_img, lr_img):
    ''' a function for mapping an image pair from [0,255]+uint8 to [-.5,.5]-float32 '''
    hr_norm = tf.cast(hr_img, tf.float32) * (1.0 / 255.0) - 0.5
    lr_norm = tf.cast(lr_img, tf.float32) * (1.0 / 255.0) - 0.5
    return hr_norm, lr_norm


def create_png_pair_dataset(input_files, batch_size, num_epochs, opts):
    ''' a function to create a data loader from input files and parameters '''
    # create a tfrecorddataset from the input files
    dataset = tf.data.TFRecordDataset(input_files)
    # map decode_png over the dataset to get decoded image pairs
    dataset = dataset.map(decode_png)
    # map normalize pair over the dataset to move the images into floating point domain
    dataset = dataset.map(normalize_pair)
    # shuffle the dataset with a buffer size multiple of the batch size
    dataset = dataset.shuffle(opts['shuffle_multiplier'] * batch_size)
    # repeat the dataset if necessary
    dataset = dataset.repeat(num_epochs)
    # divide the dataset into batches
    dataset = dataset.batch(batch_size)
    # prefetch the dataset for quicker training execution
    dataset = dataset.prefetch(opts['prefetch_size'])
    return dataset


