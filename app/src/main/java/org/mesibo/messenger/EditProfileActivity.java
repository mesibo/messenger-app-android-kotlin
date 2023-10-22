/******************************************************************************
* By accessing or copying this work, you agree to comply with the following   *
* terms:                                                                      *
*                                                                             *
* Copyright (c) 2019-2023 mesibo                                              *
* https://mesibo.com                                                          *
* All rights reserved.                                                        *
*                                                                             *
* Redistribution is not permitted. Use of this software is subject to the     *
* conditions specified at https://mesibo.com . When using the source code,    *
* maintain the copyright notice, conditions, disclaimer, and  links to mesibo * 
* website, documentation and the source code repository.                      *
*                                                                             *
* Do not use the name of mesibo or its contributors to endorse products from  *
* this software without prior written permission.                             *
*                                                                             *
* This software is provided "as is" without warranties. mesibo and its        *
* contributors are not liable for any damages arising from its use.           *
*                                                                             *
* Documentation: https://mesibo.com/documentation/                            *
*                                                                             *
* Source Code Repository: https://github.com/mesibo/                          *
*******************************************************************************/

package org.mesibo.messenger;

import android.content.Context;
import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;

import com.mesibo.api.Mesibo;


public class EditProfileActivity extends AppCompatActivity {

    Fragment mRequestingFragment;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_new_profile);

        Bundle args = getIntent().getExtras();
        if (null == args) {
            return;
        }

        long groupid = args.getLong("groupid", 0);
        boolean launchMesibo = args.getBoolean("launchMesibo", false);

        EditProfileFragment registerNewProfileFragment = new EditProfileFragment();
        registerNewProfileFragment.setGroupId(groupid);
        registerNewProfileFragment.setLaunchMesibo(launchMesibo);
        mRequestingFragment = registerNewProfileFragment;
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.register_new_profile_fragment_place, registerNewProfileFragment,"null");
        ft.addToBackStack("registerNewProfileFragment");
        ft.commit();
    }

    @Override
    public  void onResume() {
        super.onResume();
        Mesibo.setForegroundContext(this, 0x101, true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Mesibo.setForegroundContext(this, 0x101, false);
    }

    @Override
    public void onBackPressed() {
        if(getSupportFragmentManager().getBackStackEntryCount() > 1)
            getSupportFragmentManager().popBackStackImmediate();

    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm.isAcceptingText()) {
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }

        for (Fragment fragment : getSupportFragmentManager().getFragments()) {
            if(fragment instanceof EditProfileFragment && fragment.isVisible())
                fragment.onActivityResult(requestCode, resultCode, data);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(mRequestingFragment!=null)
            mRequestingFragment.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
