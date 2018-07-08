/*
 * Copyright (c) 2016. Ted Park. All Rights Reserved
 */

package com.gun0912.tedpicker;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.gun0912.tedpicker.custom.adapter.SpacesItemDecoration;
import com.gun0912.tedpicker.util.Util;
import com.simons.owner.traffickcam2.MainActivity;
import com.simons.owner.traffickcam2.R;
import java.io.IOException;
import java.util.ArrayList;

public class ImagePickerActivity extends AppCompatActivity implements
      CompoundButton.OnCheckedChangeListener {

  /**
   * Returns the parcelled image uris in the intent with this extra.
   */
  public static final String EXTRA_IMAGE_URIS = "image_uris";
  // initialize with default config.
  private static Config mConfig = new Config();

  /**
   * Key to persist the list when saving the state of the activity.
   */

  /** Hotel images taken by the user to be uploaded and submitted to the TC server **/
  public ArrayList<Uri> selectedImages;

  /** Generic Toolbar at top of ImagePickerActivity **/
  protected Toolbar toolbar;

  /** Major body of ImagePickerActivity GUI **/
  View mainViewRot;

  /** Message to let uses know they haven't selected any photos **/
  TextView selectedImageEmptyView;

  /** Scrollable view of all selected photos **/
  RecyclerView selectedPhotosRecyclerView;

  /** selectedImageEmptyView and selectedPhotosRecyclerView **/
  View selectedPhotosContainerView;

  /** Small toolbar labeling selectedPhotosContainerView **/
  TextView selectedPhotosTitleTextView;

  /** Box where non-selected photos and camera are displayed **/
  ViewPager nonselectedImagesViewPager;

  /** Tabs that allow user to switch back and forth between gallery and camera **/
  TabLayout tabLayout;

  /** TODO: This may be deprecated... Check whether or not it's needed and where **/
  PagerAdapter_Picker adapter;

  /** Adapter to help handle selected photos **/
  Adapter_SelectedPhoto selectedPhotoAdapter;

  /********************************
   * VERSION 2 VARIABLES
   ********************************/
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

  /** Returns configuration settings.**/
  public static Config getConfig() {
        return mConfig;
    }

  /**
   * @param config
   * Sets configuration settings.
   **/
  public static void setConfig(Config config) {
    if (config == null) {
      throw new NullPointerException("Config cannot be passed null. Not setting config will use default values.");
    }
    mConfig = config;
  }

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
    initView();
    makeDialogue();
    setTitle(mConfig.getToolbarTitleRes());

    setupTabs();
    setSelectedPhotoRecyclerView();

    if ( ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
     != PackageManager.PERMISSION_GRANTED) {
      ActivityCompat.requestPermissions(this, new String[]{
          Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
    }
  }

  public ImageView getImageView() {
    return selectItemsImagePreview;
  }

  private void makeDialogue() {

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
          setEnabledPositiveButton(true);
          }
          else
          unlistedHotelItemEditText.setEnabled(false);
        if (!isLabeled())
          setEnabledPositiveButton(false);
        }
    });
    imageLoadingBar = (ProgressBar) view.findViewById(R.id.progressBar);
    imageLoadingBar.setVisibility(View.VISIBLE);

    // implement button listeners within the dialog
    builder.setTitle("Are any of these items in this photo?")
      .setView(view)
      .setPositiveButton("CONFIRM", new DialogInterface.OnClickListener(){
        @Override
        public void onClick(DialogInterface dialog, int which) {
          //if(newestUri!= null) addImage(newestUri);
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

  private void initView() {
    toolbar = (Toolbar) findViewById(com.simons.owner.traffickcam2.R.id.toolbar);
    setSupportActionBar(toolbar);

    mainViewRot = findViewById(com.simons.owner.traffickcam2.R.id.view_root);
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

        int selected_bottom_size = (int) getResources().getDimension(mConfig.getSelectedBottomHeight());

        ViewGroup.LayoutParams params = selectedPhotosContainerView.getLayoutParams();
        params.height = selected_bottom_size;
        selectedPhotosContainerView.setLayoutParams(params);
        return true;
      }
    });


    if (mConfig.getSelectedBottomColor() > 0) {
      selectedPhotosTitleTextView.setBackgroundColor(ContextCompat.getColor(this, mConfig.getSelectedBottomColor()));
      selectedImageEmptyView.setTextColor(ContextCompat.getColor(this, mConfig.getSelectedBottomColor()));
    }
  }

  private void setupFromSavedInstanceState(Bundle savedInstanceState) {
    if (savedInstanceState != null) {
      selectedImages = savedInstanceState.getParcelableArrayList(EXTRA_IMAGE_URIS);
    } else {
      selectedImages = getIntent().getParcelableArrayListExtra(EXTRA_IMAGE_URIS);
    }

    if (selectedImages == null) {
      selectedImages = new ArrayList<>();
    }
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);

    if (selectedImages != null) {
      outState.putParcelableArrayList(EXTRA_IMAGE_URIS, selectedImages);
    }
  }

  private void setupTabs() {
    adapter = new PagerAdapter_Picker(this, getSupportFragmentManager());
    nonselectedImagesViewPager.setAdapter(adapter);
    tabLayout.setupWithViewPager(nonselectedImagesViewPager);

    if (mConfig.getTabBackgroundColor() > 0)
      tabLayout.setBackgroundColor(ContextCompat.getColor(this, mConfig.getTabBackgroundColor()));

    if (mConfig.getTabSelectionIndicatorColor() > 0)
      tabLayout.setSelectedTabIndicatorColor(
          ContextCompat.getColor(this, mConfig.getTabSelectionIndicatorColor()));
  }

  private void setSelectedPhotoRecyclerView() {
    LinearLayoutManager mLayoutManager_Linear = new LinearLayoutManager(this);
    mLayoutManager_Linear.setOrientation(LinearLayoutManager.HORIZONTAL);

    selectedPhotosRecyclerView.setLayoutManager(mLayoutManager_Linear);
    selectedPhotosRecyclerView.addItemDecoration(new SpacesItemDecoration(Util.dpToPx(this, 5), SpacesItemDecoration.TYPE_VERTICAL));
    selectedPhotosRecyclerView.setHasFixedSize(true);

    int closeImageRes = mConfig.getSelectedCloseImage();

    selectedPhotoAdapter = new Adapter_SelectedPhoto(this, closeImageRes);
    selectedPhotoAdapter.updateItems(selectedImages);
    selectedPhotosRecyclerView.setAdapter(selectedPhotoAdapter);


    if (selectedImages.size() >= 1) {
      selectedImageEmptyView.setVisibility(View.GONE);
    }
  }

  public GalleryFragment getGalleryFragment() {

    if (adapter == null || adapter.getCount() < 2)
      return null;

     return (GalleryFragment) adapter.getItem(1);
  }

  public void showDialog()
  {
    imageLoadingBar.setVisibility(View.VISIBLE);
    selectItemsImagePreview.setVisibility(View.INVISIBLE);
    clearCheckboxes();
    imageClassificationDialog.show();
    setEnabledPositiveButton(false);
  }

    public void setUri(final Uri uri)
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

  public void addImage(final Uri uri) {
    newestUri = uri;
    setUri(uri);
    if (selectedImages.size() == mConfig.getSelectionLimit()) {
      String text = String.format(getResources().getString(com.simons.owner.traffickcam2.R.string.max_count_msg), mConfig.getSelectionLimit());
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

    // get orientation info either from EXIF OR the Media object (a more robust solution)
    public static int getImageRotation(Context context, Uri imageUri) {
        try {
            ExifInterface exif = new ExifInterface(imageUri.getPath());
            int rotation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);

            if (rotation == ExifInterface.ORIENTATION_UNDEFINED)
                return getRotationFromMediaStore(context, imageUri);
            else return exifToDegrees(rotation);
        } catch (IOException e) {
            return 0;
        }
    }

    // get orientation from the EXIF inf
  private static int exifToDegrees(int exifOrientation) {
    if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_90) {
      return 90;
    } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_180) {
      return 180;
    } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_270) {
      return 270;
    } else {
      return 0;
    }
  }

    // get orientation info from the Media object
  public static int getRotationFromMediaStore(Context context, Uri imageUri) {
    String[] columns = {MediaStore.Images.Media.DATA, MediaStore.Images.Media.ORIENTATION};
    Cursor cursor = context.getContentResolver().query(imageUri, columns, null, null, null);
    if (cursor == null) return 0;

    cursor.moveToFirst();
    int orientationColumnIndex = cursor.getColumnIndex(columns[1]);
      return cursor.getInt(orientationColumnIndex);
  }

  private void setEnabledPositiveButton(boolean labelCheck) {
    imageClassificationDialog.getButton(AlertDialog.BUTTON_POSITIVE)
        .setEnabled(labelCheck);
  }

  private void clearCheckboxes()
  {
    int n = checkBoxes.length;
    for(int i = 0; i < n; i++) {
      if (checkBoxes[i].isChecked()) checkBoxes[i].setChecked(false);
    }
  }

  private boolean isLabeled() {
    boolean isLabeled = false;
    int checkBoxCount = checkBoxes.length;
    for (int i = 0; i < checkBoxCount && isLabeled != true; ++i)
      if (checkBoxes[i].isChecked()) isLabeled = true;
    return isLabeled;
  }

  public void removeImage(Uri uri) {
    selectedImages.remove(uri);
    selectedPhotoAdapter.updateItems(selectedImages);
    if (selectedImages.size() == 0) {
      selectedImageEmptyView.setVisibility(View.VISIBLE);
    }
    GalleryFragment.mGalleryAdapter.notifyDataSetChanged();
  }

  public boolean containsImage(Uri uri) {
    return selectedImages.contains(uri);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.menu_confirm, menu);
    return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    int id = item.getItemId();
    if (id == android.R.id.home) {
      finish();
      return true;
     } else if (id == R.id.action_done) {
       updatePicture();
       return true;
    }
    return super.onOptionsItemSelected(item);
  }

    private void updatePicture() {
        if (selectedImages.size() < 1) {
            String text = String.format(getResources().getString(com.simons.owner.traffickcam2.R.string.min_count_msg));
            Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(ImagePickerActivity.this, MainActivity.class);
        Bundle bundle = new Bundle();
        bundle.putParcelableArrayList(EXTRA_IMAGE_URIS, selectedImages);
        intent.putExtras(bundle);
        startActivityForResult(intent, 1);
    }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == 1) {
      if(resultCode == Activity.RESULT_OK){
        // CLEAR CURRENT PHOTOS
        selectedImages.clear();
        selectedPhotoAdapter.updateItems(selectedImages);
        if (selectedImages.size() == 0) {
          selectedImageEmptyView.setVisibility(View.VISIBLE);
        }
        GalleryFragment.mGalleryAdapter.notifyDataSetChanged();
      }
    }
  }

  @Override
  public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
    if (isChecked)
      setEnabledPositiveButton(true);
    else
      if (!isLabeled())
        setEnabledPositiveButton(false);
  }
}
