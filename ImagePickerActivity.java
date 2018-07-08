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

public class ImagePickerActivity extends AppCompatActivity{

  /**  Returns the parcelled image uris in the intent with this extra. **/
  public static final String EXTRA_IMAGE_URIS = "image_uris";

  /** Activity configuration initializes with default settings. **/
  private static Config configuration = new Config();

  /** Hotel images taken by the user to be uploaded and submitted to the TC server **/
  public ArrayList<Uri> selectedImages;

  /** Generic Toolbar at top of ImagePickerActivity **/
  protected Toolbar toolbar;

  /** Major body of ImagePickerActivity GUI **/
  View mainViewRoot;

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


  /** Returns configuration settings.**/
  public static Config getConfig() {
        return configuration;
    }

  /**
   * @param config
   * Sets configuration settings.
   **/
  public static void setConfig(Config config) {
    if (config == null) {
      throw new NullPointerException("Config cannot be passed null. Not setting config will use default values.");
    }
    configuration = config;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setupFromSavedInstanceState(savedInstanceState);
    setContentView(com.simons.owner.traffickcam2.R.layout.picker_activity_main_pp);
    setTitle(configuration.getToolbarTitleRes());

    setupTabs();
    setSelectedPhotoRecyclerView();

    if ( ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
     != PackageManager.PERMISSION_GRANTED) {
      ActivityCompat.requestPermissions(this, new String[]{
          Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
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

    if (configuration.getTabBackgroundColor() > 0)
      tabLayout.setBackgroundColor(ContextCompat.getColor(this, configuration.getTabBackgroundColor()));

    if (configuration.getTabSelectionIndicatorColor() > 0)
      tabLayout.setSelectedTabIndicatorColor(
          ContextCompat.getColor(this, configuration.getTabSelectionIndicatorColor()));
  }

  private void setSelectedPhotoRecyclerView() {
    LinearLayoutManager mLayoutManager_Linear = new LinearLayoutManager(this);
    mLayoutManager_Linear.setOrientation(LinearLayoutManager.HORIZONTAL);

    selectedPhotosRecyclerView.setLayoutManager(mLayoutManager_Linear);
    selectedPhotosRecyclerView.addItemDecoration(new SpacesItemDecoration(Util.dpToPx(this, 5), SpacesItemDecoration.TYPE_VERTICAL));
    selectedPhotosRecyclerView.setHasFixedSize(true);

    int closeImageRes = configuration.getSelectedCloseImage();

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

  public void addImage(final Uri uri) {
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
}
