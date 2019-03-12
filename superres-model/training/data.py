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


def create_png_pair_dataset(input_files, batch_size, opts):
    ''' a function to create a data loader from input files and parameters '''
    # create a tfrecorddataset from the input files
    dataset = tf.data.TFRecordDataset(input_files)
    # map decode_png over the dataset to get decoded image pairs
    dataset = dataset.map(decode_png)
    # map normalize pair over the dataset to move the images into floating point domain
    dataset = dataset.map(normalize_pair)
    # shuffle the dataset with a buffer size multiple of the batch size
    dataset = dataset.shuffle(opts['shuffle_multiplier'] * batch_size)
    # divide the dataset into batches
    dataset = dataset.batch(batch_size)
    # prefetch the dataset for quicker training execution
    dataset = dataset.prefetch(opts['prefetch_size'])
    return dataset


def create_combined_iterator(training_set, validation_set):
    ''' a function that creates an iterator that can be reinitialized over both datasets '''
    # create a handle for iterator feeding
    handle = tf.placeholder(tf.string, shape=[])
    # create an iterator through the handle & defined structure
    itr = tf.data.Iterator.from_string_handle(handle, training_set.output_types, 
                                              training_set.output_shapes)
    # create the tensor to get the next element (the actual iterator)
    itr_next = itr.get_next()
    # create reinitializable iterators, don't forget to get handles for both later!
    training_itr = training_set.make_initializable_iterator()
    validation_itr = validation_set.make_initializable_iterator()
    # add two extra infinite training iterators for the discriminator
    # the reason we require two iterators is that we want to get the same
    # data twice: once for training with the original data, and then
    # training with the superresolved data
    discr_set = training_set.repeat()
    discr_itr1 = discr_set.make_one_shot_iterator()
    discr_itr2 = discr_set.make_one_shot_iterator()
    # return everything
    return handle, itr_next, training_itr, validation_itr, discr_itr1, discr_itr2


def create_combined_data_loader(training_data_file, validation_data_file, 
                                batch_size, opts):
    ''' 
    This function combines create_png_pair_dataset and create_combined_iterator to
    create a data loader that can be reinitialized over both datasets, given the file
    paths as input. The returned tuple contains an iterator tensor, as well as two
    reinitialization ops. Batch_size and opts are used in the creation of both sets.
    '''
    # create both datasets and return their combined iterator
    training_dataset = create_png_pair_dataset((training_data_file,), batch_size, opts)
    validation_dataset = create_png_pair_dataset((validation_data_file,), batch_size, opts)
    combined_loader = create_combined_iterator(training_dataset, validation_dataset)
    return combined_loader

