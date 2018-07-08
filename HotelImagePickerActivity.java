
package com.simons.owner.traffickcam2;

public class HotelImagePickerActivity
  extends ImagePickerActivity
  implements CompoundButton.OnCheckedChangeListener
{

  /**  Returns the parcelled image uris in the intent with this extra. **/
  @Override
  public static final String EXTRA_IMAGE_URIS = "hotel_image_uris";

  /**
   * AlertDialog that opens up after a photo has been selected so that users may identify
   * what items are in their photo.
   */
  AlertDialog imageClassificationDialog;

  /** Checkboxes for checking which pre-listed items are in the selected photo **/
  CheckBox checkBoxes[];

  /** Preview of image most recently taken to help user select items **/
  ImageView selectItemsImagePreview;

  /** Loading bar displays when selectItemsImagePreview is loading **/
  ProgressBar imageLoadingBar;

  /** Uri file for latest selected photo **/
  public Uri newestUri = null;

  /** Edit text that allows users to specify other items in their selected photo **/
  EditText unlistedHotelItemEditText;

  /**
   * @param savedInstanceState
   *
   * Called when an instance of ImagePickerActivity is first run.
   * Calls to initialize most views and user-side components of the activity
   * Also checks that TraffickCam has permission to read external storage and
   * requests permission if that permission is not currently granted
   */
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setupFromSavedInstanceState(savedInstanceState);
    setContentView(com.simons.owner.traffickcam2.R.layout.picker_activity_main_pp);

    setUpMainViewRoot();
    setUpImageClassificationDialog();

    setTitle(configuration.getToolbarTitleRes());

    setupTabs();
    setSelectedPhotoRecyclerView();

    // check for permission to read external storage
    // ask for permission if not granted
    if ( ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
     != PackageManager.PERMISSION_GRANTED) {
      ActivityCompat.requestPermissions(this, new String[]{
          Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
    }
  }

  /** Assigns view variables **/
  private void setUpMainViewRoot() {
    toolbar = (Toolbar) findViewById(com.simons.owner.traffickcam2.R.id.toolbar);
    setSupportActionBar(toolbar);

    mainViewRoot = findViewById(com.simons.owner.traffickcam2.R.id.view_root);
    nonselectedImagesViewPager = (ViewPager) findViewById(com.simons.owner.traffickcam2.R.id.pager);
    tabLayout = (TabLayout) findViewById(com.simons.owner.traffickcam2.R.id.tab_layout);
    selectedPhotosTitleTextView = (TextView) findViewById(com.simons.owner.traffickcam2.R.id.tv_selected_title);
    selectedPhotosRecyclerView = (RecyclerView) findViewById(com.simons.owner.traffickcam2.R.id.rc_selected_photos);
    selectedImageEmptyView = (TextView) findViewById(com.simons.owner.traffickcam2.R.id.selected_photos_empty);
    selectedPhotosContainerView = findViewById(com.simons.owner.traffickcam2.R.id.view_selected_photos_container);

    selectedPhotosContainerView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
      @Override
      public boolean onPreDraw() {
        selectedPhotosContainerView.getViewTreeObserver().removeOnPreDrawListener(this);

        int selected_bottom_size = (int) getResources().getDimension(configuration.getSelectedBottomHeight());

        ViewGroup.LayoutParams params = selectedPhotosContainerView.getLayoutParams();
        params.height = selected_bottom_size;
        selectedPhotosContainerView.setLayoutParams(params);
        return true;
      }
    });

    if (configuration.getSelectedBottomColor() > 0) {
      selectedPhotosTitleTextView.setBackgroundColor(ContextCompat.getColor(this, configuration.getSelectedBottomColor()));
      selectedImageEmptyView.setTextColor(ContextCompat.getColor(this, configuration.getSelectedBottomColor()));
    }
  }

  /**
   *  @param uri
   *
   *  Adds given uri to selected images
   */
  @Override
  public void addImage(final Uri uri) {
    newestUri = uri;
    setDialogPreviewUri(uri);

    if (selectedImages.size() == configuration.getSelectionLimit()) {
      String text = String.format(getResources().getString(com.simons.owner.traffickcam2.R.string.max_count_msg), configuration.getSelectionLimit());
      Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
      return;
    }

    selectedImages.add(uri);
    selectedPhotoAdapter.updateItems(selectedImages);

    if (selectedImages.size() >= 1) {
      selectedImageEmptyView.setVisibility(View.GONE);
    }

    selectedPhotosRecyclerView.smoothScrollToPosition(selectedPhotoAdapter.getItemCount()-1);
  }

  /** Returns view of latest selected image**/
  public ImageView getImageView() {
    return selectItemsImagePreview;
  }

  /**
   *  Initalizes and populates the alert dialog that allows users to select
   *  what items are in their selected photo
   */
  private void setUpImageClassificationDialog() {

    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    LayoutInflater factory = LayoutInflater.from(ImagePickerActivity.this);
    View view = factory.inflate(R.layout.alert_dialog, null);
    selectItemsImagePreview = (ImageView) view.findViewById(R.id.dialog_imageview);
    selectItemsImagePreview.setVisibility(View.INVISIBLE);
    unlistedHotelItemEditText = (EditText) view.findViewById(R.id.Other);
    unlistedHotelItemEditText.setEnabled(false);

    checkBoxes = new CheckBox[8];
    checkBoxes[0] = (CheckBox) view.findViewById(R.id.checkBox0);
    checkBoxes[1] = (CheckBox) view.findViewById(R.id.checkBox1);
    checkBoxes[2] = (CheckBox) view.findViewById(R.id.checkBox2);
    checkBoxes[3] = (CheckBox) view.findViewById(R.id.checkBox3);
    checkBoxes[4] = (CheckBox) view.findViewById(R.id.checkBox4);
    checkBoxes[5] = (CheckBox) view.findViewById(R.id.checkBox5);
    checkBoxes[6] = (CheckBox) view.findViewById(R.id.checkBox6);
    checkBoxes[7] = (CheckBox) view.findViewById(R.id.checkBox7);

    // checkboxes 0-6 call ImagePickerActivity.onCheckedChanged
    // checkbox 7 will allow users to type in their own items
    checkBoxes[0].setOnCheckedChangeListener(this);
    checkBoxes[1].setOnCheckedChangeListener(this);
    checkBoxes[2].setOnCheckedChangeListener(this);
    checkBoxes[3].setOnCheckedChangeListener(this);
    checkBoxes[4].setOnCheckedChangeListener(this);
    checkBoxes[5].setOnCheckedChangeListener(this);
    checkBoxes[6].setOnCheckedChangeListener(this);
    checkBoxes[7].setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
    {
      @Override
      public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if(isChecked) {
          unlistedHotelItemEditText.setEnabled(true);
          setImageClassButtonEnabled(AlertDialog.BUTTON_POSITIVE, true);
        }
        else
          unlistedHotelItemEditText.setEnabled(false);
        if (!imageIsLabeled())
          setImageClassButtonEnabled(AlertDialog.BUTTON_POSITIVE,false);
        }
    });
    imageLoadingBar = (ProgressBar) view.findViewById(R.id.progressBar);
    imageLoadingBar.setVisibility(View.VISIBLE);

    // TODO: save these string literals as global strings in the project
    builder.setTitle("Are any of these items in this photo?")
      .setView(view)
      .setPositiveButton("CONFIRM", new DialogInterface.OnClickListener(){
        @Override
        public void onClick(DialogInterface dialog, int which) {
          imageLoadingBar.setVisibility(View.VISIBLE);
          selectItemsImagePreview.setVisibility(View.INVISIBLE);
        }
      })
      .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
          if (newestUri != null) removeImage(newestUri);
          imageLoadingBar.setVisibility(View.VISIBLE);
          selectItemsImagePreview.setVisibility(View.INVISIBLE);
        }
      });

    // create the dialog itself
    imageClassificationDialog = builder.create();

    // apply listener for touches outside the dialog box to remove any added image
    imageClassificationDialog.setOnCancelListener(
      new DialogInterface.OnCancelListener() {
        @Override
        public void onCancel(DialogInterface dialog) {
          if (newestUri != null) removeImage(newestUri);
          }
      }
    );
  }

  /** NOTE: Only needed for version 2 **/
  /** Checks if the user has labeled the image they wish to submit **/
  private boolean imageIsLabeled() {
    boolean isLabeled = false;
    int checkBoxCount = checkBoxes.length;
    for (int i = 0; i < checkBoxCount && isLabeled != true; ++i)
      if (checkBoxes[i].isChecked()) isLabeled = true;
    return isLabeled;
  }

  /** NOTE: Only needed for version 2 **/
  /** Shows dialog that allows users to label their selected photo**/
  public void showImageClassificationDialog()
  {
    imageLoadingBar.setVisibility(View.VISIBLE);
    selectItemsImagePreview.setVisibility(View.INVISIBLE);
    clearCheckboxes();
    imageClassificationDialog.show();
    setImageClassButtonEnabled(AlertDialog.BUTTON_POSITIVE, false);
  }

  /** NOTE: Only needed for version 2 **/
  /** Enables or disables the images on the image classification dialog **/
  private void setImageClassButtonEnabled(final int BUTTON, boolean labelCheck) {
    imageClassificationDialog.getButton(BUTTON)
        .setEnabled(labelCheck);
  }

  /** NOTE: Only needed for version 2 **/
  /** Enables ability to submit photos based on whether or not they've been labeled **/
  @Override
  public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
    if (isChecked)
      setImageClassButtonEnabled(AlertDialog.BUTTON_POSITIVE, true);
    else
      if (!imageIsLabeled())
        setImageClassButtonEnabled(AlertDialog.BUTTON_POSITIVE, false);
  }

  /** NOTE: Only needed for version 2 **/
  /**
   *  Clears all the checkboxes on the image classification dialog so that 
   *  the user may label new images
   */
  private void clearCheckboxes()
  {
    int n = checkBoxes.length;
    for(int i = 0; i < n; i++) {
      if (checkBoxes[i].isChecked()) checkBoxes[i].setChecked(false);
    }
  }

  /** NOTE: Only needed for version 2 **/
  /**
   * @param uri
   *
   * Sets preview image on image classification dialog
   */ 
  public void setDialogPreviewUri(final Uri uri)
  {
    selectItemsImagePreview.setImageURI(uri);
    imageLoadingBar.setVisibility(View.INVISIBLE);

    // re-orient selected image to counter image rotation bug
    int orientation = 0;
    try {
      assert uri != null;
      orientation = getImageRotation(this, uri);
    } catch (Exception e) {
      e.printStackTrace();
    }
      selectItemsImagePreview.setRotation(orientation);
      selectItemsImagePreview.setVisibility(View.VISIBLE);
    }
  }
}
