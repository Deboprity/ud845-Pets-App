/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.pets;

import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.android.pets.data.PetContract.PetEntry;

import org.apache.commons.lang3.StringUtils;

import java.net.URI;

/**
 * Allows user to create a new pet or edit an existing one.
 */
public class EditorActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>{

    private static final String TAG = EditorActivity.class.getSimpleName();
    /** EditText field to enter the pet's name */
    private EditText mNameEditText;

    /** EditText field to enter the pet's breed */
    private EditText mBreedEditText;

    /** EditText field to enter the pet's weight */
    private EditText mWeightEditText;

    /** EditText field to enter the pet's gender */
    private Spinner mGenderSpinner;

    /**
     * Gender of the pet. The possible valid values are in the PetContract.java file:
     * {@link PetEntry#GENDER_UNKNOWN}, {@link PetEntry#GENDER_MALE}, or
     * {@link PetEntry#GENDER_FEMALE}.
     */
    private int mGender = 99;

    private static final int PET_EDITOR_LOADER = 1;

    /** Content URI for the existing pet (null if it's a new pet) */
    private Uri mCurrentPetUri;

    private boolean mPetHasChanged = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate: started");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        Intent intent = getIntent();
        Uri currentPetUri = intent.getData();

        Log.d(TAG, "onCreate: currentPetUri :: "+currentPetUri);

        // Find all relevant views that we will need to read user input from
        mNameEditText = (EditText) findViewById(R.id.edit_pet_name);
        mBreedEditText = (EditText) findViewById(R.id.edit_pet_breed);
        mWeightEditText = (EditText) findViewById(R.id.edit_pet_weight);
        mGenderSpinner = (Spinner) findViewById(R.id.spinner_gender);

        setupSpinner();

        if(currentPetUri == null){
            setTitle(getString(R.string.editor_activity_title_new_pet));
            // Invalidate the options menu, so the "Delete" menu option can be hidden.
            // (It doesn't make sense to delete a pet that hasn't been created yet.)
            invalidateOptionsMenu();
        }else{
            setTitle(getString(R.string.editor_activity_title_edit_pet));
            mCurrentPetUri = currentPetUri;
            getLoaderManager().initLoader(PET_EDITOR_LOADER, null, this);
        }

        mNameEditText.setOnTouchListener(mTouchListener);
        mBreedEditText.setOnTouchListener(mTouchListener);
        mWeightEditText.setOnTouchListener(mTouchListener);
        mGenderSpinner.setOnTouchListener(mTouchListener);

        Log.d(TAG, "onCreate: ended");
    }

    // OnTouchListener that listens for any user touches on a View, implying that they are modifying
    // the view, and we change the mPetHasChanged boolean to true.

    private View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            mPetHasChanged = true;
            return false;
        }
    };

    /**
     * Setup the dropdown spinner that allows the user to select the gender of the pet.
     */
    private void setupSpinner() {
        Log.d(TAG, "setupSpinner: started");
        // Create adapter for spinner. The list options are from the String array it will use
        // the spinner will use the default layout
        ArrayAdapter genderSpinnerAdapter = ArrayAdapter.createFromResource(this,
                R.array.array_gender_options, android.R.layout.simple_spinner_item);

        // Specify dropdown layout style - simple list view with 1 item per line
        genderSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);

        // Apply the adapter to the spinner
        mGenderSpinner.setAdapter(genderSpinnerAdapter);

        // Set the integer mSelected to the constant values
        mGenderSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "mGenderSpinner.onItemSelected: started");
                String selection = (String) parent.getItemAtPosition(position);
                if (!TextUtils.isEmpty(selection)) {
                    if (selection.equals(getString(R.string.gender_male))) {
                        mGender = PetEntry.GENDER_MALE;
                    } else if (selection.equals(getString(R.string.gender_female))) {
                        mGender = PetEntry.GENDER_FEMALE;
                    } else if (selection.equals(getString(R.string.gender_unknown))){
                        mGender = PetEntry.GENDER_UNKNOWN;
                    } else{
                        mGender = 99;
                    }
                }
                Log.d(TAG, "onItemSelected: mGender :: "+mGender);
            }

            // Because AdapterView is an abstract class, onNothingSelected must be defined
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                mGender = 99;
            }
        });
        Log.d(TAG, "setupSpinner: ended");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(TAG, "onCreateOptionsMenu: started");
        // Inflate the menu options from the res/menu/menu_editor.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.menu_editor, menu);
        Log.d(TAG, "onCreateOptionsMenu: ended");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG, "onOptionsItemSelected: started");
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            // Respond to a click on the "Save" menu option
            case R.id.action_save:
                if(null == mCurrentPetUri){
                    // Save pet to database if the flow is "Add a Pet"
                    insertPet();
                }else{
                    // Update the pet to database if the flow is "Edit Pet"
                    updatePet();
                }
                // Exit activity
                finish();
                return true;
            // Respond to a click on the "Delete" menu option
            case R.id.action_delete:
                showDeleteConfirmationDialog();
                return true;
            // Respond to a click on the "Up" arrow button in the app bar
            case android.R.id.home:
                // If the pet hasn't changed, continue with navigating up to parent activity
                // which is the {@link CatalogActivity}.
                if (!mPetHasChanged) {
                    NavUtils.navigateUpFromSameTask(EditorActivity.this);
                    return true;
                }

                // Otherwise if there are unsaved changes, setup a dialog to warn the user.
                // Create a click listener to handle the user confirming that
                // changes should be discarded.
                DialogInterface.OnClickListener discardButtonClickListener =
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // User clicked "Discard" button, navigate to parent activity.
                                NavUtils.navigateUpFromSameTask(EditorActivity.this);
                            }
                        };

                // Show a dialog that notifies the user they have unsaved changes
                showUnsavedChangesDialog(discardButtonClickListener);
                return true;
        }
        Log.d(TAG, "onOptionsItemSelected: ended");
        return super.onOptionsItemSelected(item);
    }

    /*
       *Get user input from editor and save new pet into database
    */
    private void insertPet(){
        Log.d(TAG, "insertPet: started");
        ContentValues values = readUserInput();

        if (null == mCurrentPetUri) {
            if(StringUtils.isEmpty(values.getAsString(PetEntry.COLUMN_PET_NAME)) && StringUtils.isEmpty(values.getAsString(PetEntry.COLUMN_PET_BREED)) && StringUtils.isEmpty(values.getAsString(PetEntry.COLUMN_PET_WEIGHT)) && mGender == PetEntry.GENDER_UNKNOWN){
                return;
            }
        }

        // Insert a new pet into the provider, returning the content URI for the new pet.
        Uri newUri = getContentResolver().insert(PetEntry.CONTENT_URI, values);
        Log.d(TAG, "insertPet: newUri :: "+newUri);
        // Show a toast message depending on whether or not the insertion was successful
        if(newUri == null){
            // If the new content URI is null, then there was an error with insertion.
            Toast.makeText(this, getString(R.string.editor_insert_pet_failed), Toast.LENGTH_SHORT).show();
        }else{
            // Otherwise, the insertion was successful and we can display a toast.
            Toast.makeText(this, getString(R.string.editor_insert_pet_successful), Toast.LENGTH_SHORT).show();
        }
    }

    /*
     *Get user input from editor and update changes into database
     */
    private void updatePet(){
        Log.d(TAG, "updatePet: started");
        ContentValues values = readUserInput();

        // Update the pet into the provider, returning the content URI for the pet.
        int noOfRowsUpdated = getContentResolver().update(mCurrentPetUri, values, null, null);
        Log.d(TAG, "updatePet: noOfRowsUpdated :: "+noOfRowsUpdated);
        // Show a toast message depending on whether or not the update was successful
        if(noOfRowsUpdated < 1){
            // If the new content URI is null, then there was an error with update.
            Toast.makeText(this, getString(R.string.editor_update_pet_failed), Toast.LENGTH_SHORT).show();
        }else{
            // Otherwise, the update was successful and we can display a toast.
            Toast.makeText(this, getString(R.string.editor_update_pet_successful), Toast.LENGTH_SHORT).show();
        }

        Log.d(TAG, "updatePet: ended");
    }

    private ContentValues readUserInput(){
        Log.d(TAG, "readUserInput: started");
        ContentValues values = new ContentValues();
        // Read from input fields
        // Use trim to eliminate leading or trailing white spaces
        String nameString, breedString, weightString;
        int weight = 0;
        if(null != mNameEditText.getText()) {
            nameString = mNameEditText.getText().toString().trim();
            values.put(PetEntry.COLUMN_PET_NAME, nameString);
        }
        if(null != mBreedEditText.getText()) {
            breedString = mBreedEditText.getText().toString().trim();
            values.put(PetEntry.COLUMN_PET_BREED, breedString);
        }
        weightString = mWeightEditText.getText().toString().trim();
        if(StringUtils.isNotEmpty(weightString)){
            weight = Integer.parseInt(weightString);
            values.put(PetEntry.COLUMN_PET_WEIGHT, weight);
        }

        values.put(PetEntry.COLUMN_PET_GENDER, mGender);

        Log.d(TAG, "readUserInput: ended");
        return values;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Log.d(TAG, "onCreateLoader: started");
        // Since the editor shows all pet attributes, define a projection that contains
        // all columns from the pet table
        String[] projection = {
                PetEntry._ID,
                PetEntry.COLUMN_PET_NAME,
                PetEntry.COLUMN_PET_BREED,
                PetEntry.COLUMN_PET_GENDER,
                PetEntry.COLUMN_PET_WEIGHT };
        Log.d(TAG, "onCreateLoader: projection :: "+projection.length);
        Log.d(TAG, "onCreateLoader: mCurrentPetUri :: "+mCurrentPetUri);
        // This loader will execute the ContentProvider's query method on a background thread
        return new CursorLoader(this,   // Parent activity context
                mCurrentPetUri,         // Query the content URI for the current pet
                projection,             // Columns to include in the resulting Cursor
                null,                   // No selection clause
                null,                   // No selection arguments
                null);                  // Default sort order
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        Log.d(TAG, "onLoadFinished: started");
        if (cursor.moveToFirst()) {
            // Find the columns of pet attributes that we're interested in
            int nameColumnIndex = cursor.getColumnIndex(PetEntry.COLUMN_PET_NAME);
            int breedColumnIndex = cursor.getColumnIndex(PetEntry.COLUMN_PET_BREED);
            int genderColumnIndex = cursor.getColumnIndex(PetEntry.COLUMN_PET_GENDER);
            int weightColumnIndex = cursor.getColumnIndex(PetEntry.COLUMN_PET_WEIGHT);

            // Extract out the value from the Cursor for the given column index
            String name = cursor.getString(nameColumnIndex);
            String breed = cursor.getString(breedColumnIndex);
            int gender = cursor.getInt(genderColumnIndex);
            int weight = cursor.getInt(weightColumnIndex);

            // Update the views on the screen with the values from the database
            mNameEditText.setText(name);
            mBreedEditText.setText(breed);
            mWeightEditText.setText(Integer.toString(weight));

            // Gender is a dropdown spinner, so map the constant value from the database
            // into one of the dropdown options (0 is Unknown, 1 is Male, 2 is Female).
            // Then call setSelection() so that option is displayed on screen as the current selection.
            switch (gender) {
                case PetEntry.GENDER_MALE:
                    mGenderSpinner.setSelection(2);
                    break;
                case PetEntry.GENDER_FEMALE:
                    mGenderSpinner.setSelection(3);
                    break;
                case PetEntry.GENDER_UNKNOWN:
                    mGenderSpinner.setSelection(1);
                    break;
                default:
                    mGenderSpinner.setSelection(0);
                    break;
            }
        }
        Log.d(TAG, "onLoadFinished: ended");
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        Log.d(TAG, "onLoaderReset: started");
        //mCursorAdapter.swapCursor(null);
        Log.d(TAG, "onLoaderReset: ended");
    }

    private void showUnsavedChangesDialog(
            DialogInterface.OnClickListener discardButtonClickListener) {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the positive and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.unsaved_changes_dialog_msg);
        builder.setPositiveButton(R.string.discard, discardButtonClickListener);
        builder.setNegativeButton(R.string.keep_editing, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Keep editing" button, so dismiss the dialog
                // and continue editing the pet.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    @Override
    public void onBackPressed() {
        // If the pet hasn't changed, continue with handling back button press
        if (!mPetHasChanged) {
            super.onBackPressed();
            return;
        }

        // Otherwise if there are unsaved changes, setup a dialog to warn the user.
        // Create a click listener to handle the user confirming that changes should be discarded.
        DialogInterface.OnClickListener discardButtonClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // User clicked "Discard" button, close the current activity.
                        finish();
                    }
                };

        // Show dialog that there are unsaved changes
        showUnsavedChangesDialog(discardButtonClickListener);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        // If this is a new pet, hide the "Delete" menu item.
        if (mCurrentPetUri == null) {
            MenuItem menuItem = menu.findItem(R.id.action_delete);
            menuItem.setVisible(false);
        }
        return true;
    }

    private void showDeleteConfirmationDialog() {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the postivie and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_dialog_msg);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Delete" button, so delete the pet.
                deletePet();
                finish();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Cancel" button, so dismiss the dialog
                // and continue editing the pet.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * Perform the deletion of the pet in the database.
     */
    private void deletePet() {
        Log.d(TAG, "deletePet: started");
        // Delete the pet from the provider, returning the number of rows deleted.
        int noOfRowsDeleted = getContentResolver().delete(mCurrentPetUri, null, null);
        Log.d(TAG, "updatePet: noOfRowsDeleted :: "+noOfRowsDeleted);
        // Show a toast message depending on whether or not the delete was successful
        if(noOfRowsDeleted < 1){
            // If the noOfRowsDeleted is less than 1, then there was an error with delete.
            Toast.makeText(this, getString(R.string.editor_delete_pet_failed), Toast.LENGTH_SHORT).show();
        }else{
            // Otherwise, the delete was successful and we can display a toast.
            Toast.makeText(this, getString(R.string.editor_delete_pet_successful), Toast.LENGTH_SHORT).show();
        }
        Log.d(TAG, "deletePet: ended");
    }
}