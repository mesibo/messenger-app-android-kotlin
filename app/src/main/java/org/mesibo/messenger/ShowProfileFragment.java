/** Copyright (c) 2021 Mesibo
 * https://mesibo.com
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the terms and condition mentioned on https://mesibo.com
 * as well as following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this list
 * of conditions, the following disclaimer and links to documentation and source code
 * repository.
 *
 * Redistributions in binary form must reproduce the above copyright notice, this
 * list of conditions and the following disclaimer in the documentation and/or other
 * materials provided with the distribution.
 *
 * Neither the name of Mesibo nor the names of its contributors may be used to endorse
 * or promote products derived from this software without specific prior written
 * permission.
 *
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * Documentation
 * https://mesibo.com/documentation/
 *
 * Source Code Repository
 * https://github.com/mesibo/messenger-app-android
 *
 */

package org.mesibo.messenger;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.SwitchCompat;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.mesibo.api.Mesibo;
import com.mesibo.api.MesiboGroupProfile;
import com.mesibo.api.MesiboMessage;
import com.mesibo.api.MesiboProfile;
import com.mesibo.api.MesiboReadSession;
import com.mesibo.emojiview.EmojiconTextView;

import java.util.ArrayList;

import com.mesibo.mediapicker.AlbumListData;
import com.mesibo.mediapicker.AlbumPhotosData;
import com.mesibo.messaging.MesiboUI;
import com.mesibo.messaging.RoundImageDrawable;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.mesibo.messaging.MesiboUserListFragment.MODE_EDITGROUP;


public class ShowProfileFragment extends Fragment implements Mesibo.MessageListener, MesiboProfile.Listener, Mesibo.GroupListener {
    private static final int MAX_THUMBNAIL_GALERY_SIZE = 35;

    private static MesiboProfile mUser;
    private OnFragmentInteractionListener mListener;
    private ArrayList<String> mThumbnailMediaFiles;
    private LinearLayout mGallery;
    private int mMediaFilesCounter=0;
    private TextView mMediaCounterView;
    private ArrayList<AlbumListData>  mGalleryData;
    private ImageView mMessageBtn;
    private CardView mMediaCardView;
    private CardView mStatusPhoneCard;
    private CardView mGroupMembersCard;
    private CardView mExitGroupCard;
    private TextView mExitGroupText;
    private static int VIDEO_FILE = 2;
    private static int IMAGE_FILE = 1;
    private static int OTHER_FILE = 2;



    RecyclerView mRecyclerView ;
    RecyclerView.Adapter mAdapter;
    LinearLayout mAddMemebers, mEditGroup;
    ArrayList<MesiboGroupProfile.Member> mGroupMemberList = new ArrayList<>();
    MesiboGroupProfile.Member mSelfMember;

    LinearLayout mll ;
    TextView mStatus ;
    TextView mStatusTime ;
    TextView mMobileNumber ;
    TextView mPhoneType ;

    private static Bitmap mDefaultProfileBmp;
    private MesiboReadSession mReadSession = null;

    public ShowProfileFragment() {
    }

    public static ShowProfileFragment newInstance(MesiboProfile userdata) {
        ShowProfileFragment fragment = new ShowProfileFragment();
        mUser = userdata;
        mUser.addListener(fragment);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {
        setHasOptionsMenu(true);

        View v =  inflater.inflate(R.layout.fragment_show_user_profile_details, container, false);

        mDefaultProfileBmp = BitmapFactory.decodeResource(getActivity().getResources(), R.drawable.default_user_image);
        mThumbnailMediaFiles = new ArrayList<>();
        mGalleryData = new ArrayList<>();

        AlbumListData Images = new AlbumListData();
        Images.setmAlbumName("Images");
        AlbumListData Video = new AlbumListData();
        Video.setmAlbumName("Videos");
        AlbumListData Documents = new AlbumListData();
        Documents.setmAlbumName("Documents");
        mGalleryData.add(Images);
        mGalleryData.add(Video);
        mGalleryData.add(Documents);

        mMediaCardView = (CardView) v.findViewById(R.id.up_media_layout);
        mMediaCardView.setVisibility(GONE);
        Mesibo.addListener(this);

        mReadSession = mUser.createReadSession(this);
        mReadSession.enableFiles(true);
        mReadSession.enableReadReceipt(true);
        mReadSession.read(100);

        mMessageBtn = (ImageView) v.findViewById (R.id.up_message_btn);
        mMessageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().finish();
            }
        });
        mRecyclerView = (RecyclerView) v.findViewById(R.id.showprofile_memebers_rview);

        // change in file
        mAddMemebers = (LinearLayout) v.findViewById(R.id.showprofile_add_member);
        mAddMemebers.setVisibility(GONE);

        mEditGroup = (LinearLayout) v.findViewById(R.id.showprofile_editgroup);
        mEditGroup.setVisibility(GONE);


        mll = (LinearLayout) v.findViewById(R.id.up_status_card);
        mStatus = (TextView)v.findViewById(R.id.up_status_text);
        mStatusTime =(TextView) v.findViewById(R.id.up_status_update_time);
        mMobileNumber =(TextView) v.findViewById(R.id.up_number);
        mPhoneType =(TextView) v.findViewById(R.id.up_phone_type);

        mRecyclerView.setLayoutManager(new LinearLayoutManager(mRecyclerView.getContext()));
        mAdapter = new GroupMemeberAdapter(getActivity(), mGroupMemberList);
        mRecyclerView.setAdapter(mAdapter);
        ///
        mGallery = (LinearLayout) v.findViewById(R.id.up_gallery);
        mMediaCounterView = (TextView) v.findViewById(R.id.up_media_counter);
        mMediaCounterView.setText(String.valueOf(mMediaFilesCounter)+"\u3009 ");

        mStatusPhoneCard = (CardView) v.findViewById(R.id.status_phone_card) ;
        mGroupMembersCard = (CardView) v.findViewById(R.id.showprofile_members_card) ;
        mExitGroupCard = (CardView) v.findViewById(R.id.group_exit_card);
        mExitGroupText = (TextView) v.findViewById(R.id.group_exit_text);
        mExitGroupCard.setVisibility(GONE);
        mExitGroupCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mSelfMember.isOwner())
                    mUser.getGroupProfile().deleteGroup();
                else
                    mUser.getGroupProfile().leave();

                getActivity().finish();
            }
        });

        CardView e2ecard = (CardView) v.findViewById(R.id.e2ee_card);
        e2ecard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MesiboUI.showEndToEndEncryptionInfo(getActivity(), mUser.getAddress(), mUser.getGroupId());
            }
        });

        SwitchCompat switchCompat = (SwitchCompat)v.findViewById(R.id.block_m_switch);
        switchCompat.setChecked(mUser.isMessageBlocked());
        switchCompat.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mUser.blockMessages(isChecked);
                mUser.save();
            }
        });

        SwitchCompat blockGroupSwitch = (SwitchCompat)v.findViewById(R.id.block_g_switch);
        blockGroupSwitch.setChecked(mUser.isGroupMessageBlocked());
        blockGroupSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mUser.blockGroupMessages(isChecked);
                mUser.save();
            }
        });

        SwitchCompat blockVideoCallSwitch = (SwitchCompat)v.findViewById(R.id.block_v_switch);
        blockVideoCallSwitch.setChecked(mUser.isVideoCallBlocked());
        blockVideoCallSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mUser.blockVideoCalls(isChecked);
                mUser.save();
            }
        });

        SwitchCompat blockCallSwitch = (SwitchCompat)v.findViewById(R.id.block_c_switch);
        blockCallSwitch.setChecked(mUser.isCallBlocked());
        blockCallSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // if calls are blocked, video calls are blcoked too amd hence switch has no meaning
                v.findViewById(R.id.block_v_layout).setVisibility(isChecked?GONE:VISIBLE);
                mUser.blockCalls(isChecked);
                mUser.save();
            }
        });

        SwitchCompat blockProfileSwitch = (SwitchCompat)v.findViewById(R.id.block_p_switch);
        blockProfileSwitch.setChecked(mUser.isProfileSubscriptionBlocked());
        blockProfileSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(!isChecked) mUser.subscribe(true);
                mUser.blockProfileSubscription(isChecked);
                mUser.save();
            }
        });


        if(mUser.isGroup()) {
            v.findViewById(R.id.block_layout).setVisibility(GONE);
        }

        LinearLayout linearLayout = (LinearLayout) v.findViewById(R.id.up_open_media);
        linearLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(mGalleryData.size() > 0) {
                    for( int i=mGalleryData.size()-1;i>=0;i--){
                        AlbumListData tempdata = mGalleryData.get(i);
                        if(tempdata.getmPhotoCount()==0)
                            mGalleryData.remove(tempdata);
                    }

                    UIManager.launchAlbum(getActivity(), mGalleryData);
                }
            }
        });

        return v;
    }

    private void addThumbnailToGallery(MesiboMessage msg) {
        View thumbnailView = null;
        String path = msg.getFilePath();
        mThumbnailMediaFiles.add(path);
        if (mThumbnailMediaFiles.size() < MAX_THUMBNAIL_GALERY_SIZE) {
            if (null != path) {
                thumbnailView = getThumbnailView(msg.getThumbnail(), msg.hasVideo());
                if(null != thumbnailView) {
                    thumbnailView.setClickable(true);
                    thumbnailView.setTag(mMediaFilesCounter - 1);
                    thumbnailView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            int index = (int) v.getTag();
                            //String path = (String) mThumbnailMediaFiles.get(index);
                            UIManager.launchImageViewer(getActivity(), mThumbnailMediaFiles, index);
                        }
                    });
                    mGallery.addView(thumbnailView);
                }
            }
        }
    }


    View       getThumbnailView (Bitmap bm, Boolean isVideo) {
        Context activity = getActivity();
        if(null == activity) return null;
        LayoutInflater layoutInflater = LayoutInflater.from(activity);
        View view  = layoutInflater.inflate(R.layout.video_layer_layout_horizontal_gallery, null, false);
        ImageView thumbpic = (ImageView) view.findViewById(R.id.mp_thumbnail);
        thumbpic.setImageBitmap(bm);
        //thumbpic.setScaleType(ImageView.ScaleType.CENTER_CROP);
        ImageView layer = (ImageView) view.findViewById(R.id.video_layer);
        layer.setVisibility(isVideo?VISIBLE:GONE);
        DisplayMetrics metrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int width = (int)((metrics.widthPixels-50)/(5)); //number of pics in media view
        view.setLayoutParams(new ViewGroup.LayoutParams(width,width));
        return  view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.user_profile_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                getActivity().onBackPressed();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void Mesibo_onMessage(MesiboMessage msg) {
        if(!msg.hasFile()) return;
        mMediaCardView.setVisibility(VISIBLE);
        mMediaFilesCounter++;
        mMediaCounterView.setText(String.valueOf(mMediaFilesCounter)+"\u3009 ");
        AlbumPhotosData newPhoto = new AlbumPhotosData();
        newPhoto.setmPictueUrl(msg.getFilePath());
        newPhoto.setmSourceUrl(msg.getFilePath());
        AlbumListData tempAlbum;
        int index=0;
        if(msg.hasVideo())
            index = 1;
        else if (!msg.hasImage())
            index = 2;
        tempAlbum = mGalleryData.get(index);

        if(tempAlbum.getmPhotosList()==null) {
            ArrayList<AlbumPhotosData> newPhotoList = new ArrayList<>();
            tempAlbum.setmPhotosList(newPhotoList);
        }
        if(tempAlbum.getmPhotosList().size()==0) {
            tempAlbum.setmAlbumPictureUrl(msg.getFilePath());
        }
        tempAlbum.getmPhotosList().add(newPhoto);
        tempAlbum.setmPhotoCount(tempAlbum.getmPhotosList().size());
        addThumbnailToGallery(msg);
        return;
    }

    @Override
    public void Mesibo_onMessageStatus(MesiboMessage msg) {

    }

    @Override
    public void Mesibo_onMessageUpdate(MesiboMessage mesiboMessage) {

    }

    public boolean parseGroupMembers(MesiboGroupProfile.Member[] users) {
        if(null == users) return false;

        String phone = Mesibo.getAddress();
        if(TextUtils.isEmpty(phone))
            return false;

        mGroupMemberList.clear();

        for(int i=0; i < users.length; i++) {
            String peer = users[i].getAddress();
            if(phone.equalsIgnoreCase(peer)) {
                mSelfMember = users[i];
            }

            mGroupMemberList.add(users[i]);
        }

        if(null == mSelfMember) {
            mExitGroupText.setVisibility(GONE);
            mAddMemebers.setVisibility(GONE);
            mEditGroup.setVisibility(GONE);
            mAdapter.notifyDataSetChanged();
            return true;
        }

        //only owner can delete group
        mExitGroupText.setText(mSelfMember.isOwner() ? "Delete Group" : "Exit Group");

        if(mUser.groupid > 0) {
            mAddMemebers.setVisibility(mSelfMember.isAdmin() && mUser.isActive() ? VISIBLE : GONE);
            mEditGroup.setVisibility(mUser.getGroupProfile().canModify() ? VISIBLE : GONE);
        }

        mAdapter.notifyDataSetChanged();
        return true;
    }

    public void updateMember(MesiboGroupProfile.Member m) {
        for(int i=0; i < mGroupMemberList.size(); i++) {
            MesiboGroupProfile.Member em = mGroupMemberList.get(i);
            if(em.getAddress().equalsIgnoreCase(m.getAddress())) {
                mGroupMemberList.remove(em);
                mGroupMemberList.add(i, m);
                break;
            }
        }
    }

    @Override
    public void Mesibo_onGroupCreated(MesiboProfile mesiboProfile) {

    }

    @Override
    public void Mesibo_onGroupJoined(MesiboProfile mesiboProfile) {

    }

    @Override
    public void Mesibo_onGroupLeft(MesiboProfile mesiboProfile) {

    }

    @Override
    public void Mesibo_onGroupMembers(MesiboProfile mesiboProfile, MesiboGroupProfile.Member[] members) {
        parseGroupMembers(members);
    }

    @Override
    public void Mesibo_onGroupMembersJoined(MesiboProfile mesiboProfile, MesiboGroupProfile.Member[] members) {
        if(null == members) return;

        for(MesiboGroupProfile.Member m : members) {
            updateMember(m);
        }
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void Mesibo_onGroupMembersRemoved(MesiboProfile mesiboProfile, MesiboGroupProfile.Member[] members) {

    }

    @Override
    public void Mesibo_onGroupSettings(MesiboProfile mesiboProfile, MesiboGroupProfile.GroupSettings groupSettings, MesiboGroupProfile.MemberPermissions memberPermissions, MesiboGroupProfile.GroupPin[] groupPins) {

    }

    @Override
    public void Mesibo_onGroupError(MesiboProfile mesiboProfile, long l) {

    }

    @Override
    public void MesiboProfile_onUpdate(MesiboProfile userProfile) {
        if(null != mAdapter)
            mAdapter.notifyDataSetChanged();
    }

    @Override
    public void MesiboProfile_onEndToEndEncryption(MesiboProfile mesiboProfile, int i) {

    }

    @Override
    public void onResume  () {
        super.onResume();
        if(mUser.isGroup()){
            boolean isActive = mUser.isActive();
            mExitGroupCard.setVisibility(isActive?VISIBLE:GONE);
            mAddMemebers.setVisibility(isActive?VISIBLE:GONE);
            mGroupMembersCard.setVisibility(VISIBLE);
            mStatusPhoneCard.setVisibility(GONE);
            mAddMemebers.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    MesiboUI.MesiboUserListScreenOptions opts = new MesiboUI.MesiboUserListScreenOptions();
                    opts.mode = MODE_EDITGROUP;
                    opts.groupid = mUser.groupid;

                    UIManager.launchMesibo(getActivity(), opts);
                    getActivity().finish();
                }
            });

            mEditGroup.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    UIManager.launchEditProfile(getActivity(), 0, mUser.groupid, false);
                    //UIManager.launchMesiboContacts(getActivity(), 0, MODE_EDITGROUP, 0,bundle);
                    getActivity().finish();
                }
            });


            mUser.getGroupProfile().getMembers(100, true, this);

        } else {
            mExitGroupCard.setVisibility(GONE);
            mGroupMembersCard.setVisibility(GONE);
            mStatusPhoneCard.setVisibility(VISIBLE);

            if(TextUtils.isEmpty(mUser.getStatus())) {
                mll.setVisibility(GONE);
            } else {
                mll.setVisibility(VISIBLE);
                mStatus.setText(mUser.getStatus());
            }

            mStatusTime.setText((""));
            mMobileNumber.setText((mUser.address));
            mPhoneType.setText("Mobile");
        }

    }


    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(Uri uri);
    }

    public class GroupMemeberAdapter
            extends RecyclerView.Adapter<RecyclerView.ViewHolder>{
        private Context mContext=null;
        private ArrayList<MesiboGroupProfile.Member> mDataList=null;

        public GroupMemeberAdapter(Context context,ArrayList<MesiboGroupProfile.Member> list) {
            this.mContext = context;
            mDataList = list;
        }

        public   class GroupMembersCellsViewHolder extends RecyclerView.ViewHolder  {
            public String mBoundString=null;
            public View mView=null;
            public ImageView mContactsProfile=null;
            public TextView mContactsName=null;
            public TextView mAdminTextView=null;
            public EmojiconTextView mContactsStatus=null;

            public GroupMembersCellsViewHolder(View view) {
                super(view);
                mView = view;
                mContactsProfile = (ImageView) view.findViewById(R.id.sp_rv_profile);
                mContactsName = (TextView) view.findViewById(R.id.sp_rv_name);
                mContactsStatus = (EmojiconTextView) view.findViewById(R.id.sp_memeber_status);
                mAdminTextView = (TextView)  view.findViewById(R.id.admin_info);
            }
        }


        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.showprofile_group_member_rv_item, parent, false);
            return new GroupMembersCellsViewHolder(view);

        }


        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holderr, final int position) {
            final int pos = position;
            final MesiboGroupProfile.Member member = mDataList.get(position);
            final MesiboProfile user = member.getProfile();
            final GroupMembersCellsViewHolder holder = (GroupMembersCellsViewHolder) holderr;



            holder.mContactsName.setText(user.getNameOrAddress("+"));

            Bitmap memberImage = user.getImage();
            if(null != memberImage)
                holder.mContactsProfile.setImageDrawable(new RoundImageDrawable(memberImage));
            else
                holder.mContactsProfile.setImageDrawable(new RoundImageDrawable(mDefaultProfileBmp));

            if (member.isAdmin()) {
                holder.mAdminTextView.setVisibility(VISIBLE);
            }else {
                holder.mAdminTextView.setVisibility(GONE);
            }

            String status =  user.getStatus();
            if(TextUtils.isEmpty(user.getStatus())) {
                status = "";
            }

            holder.mContactsStatus.setText(status);

            // only admin can have menu, also owner can't be deleted

                holder.mView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        final MesiboGroupProfile.Member member = mDataList.get(position);
                        final MesiboProfile profile = member.getProfile();

                        if(!mSelfMember.isAdmin()) {
                            if(profile.isSelfProfile()) {
                                return;
                            }

                            MesiboUI.MesiboMessageScreenOptions opts = new MesiboUI.MesiboMessageScreenOptions();
                            opts.profile = profile;

                            MesiboUI.launchMessaging(getActivity(), opts);
                            getActivity().finish();
                            return;
                        }

                        ArrayList<String> items = new ArrayList<String>();

                        if(!member.isAdmin()) {
                            items.add("Make Admin");

                        } else {
                            items.add("Remove Admin");
                        }

                        // don't allow self messaging or self delete member
                        if(!profile.isSelfProfile()) {
                            items.add("Delete member");
                            items.add("Message");
                        }

                        CharSequence[] cs = items.toArray(new CharSequence[items.size()]);

                        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                        //builder.setTitle("Select The Action");
                        builder.setItems(cs, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int item) {
                                //Delete member
                                if (item == 1) {
                                    String[] members = new String[1];
                                    members[0] = mDataList.get(position).getAddress();
                                    mUser.getGroupProfile().removeMembers(members);
                                    mDataList.remove(position);
                                    notifyItemRemoved(position);
                                    notifyDataSetChanged();

                                } else if(item == 0 ) {
                                    String[] members = new String[1];
                                    members[0] = mDataList.get(position).getAddress();
                                    mUser.getGroupProfile().addMembers(members , MesiboGroupProfile.MEMBERFLAG_ALL, member.isAdmin()?0:MesiboGroupProfile.ADMINFLAG_ALL);
                                } else  if( 2 == item) {
                                    MesiboUI.MesiboMessageScreenOptions opts = new MesiboUI.MesiboMessageScreenOptions();
                                    opts.profile = profile;
                                    MesiboUI.launchMessaging(getActivity(), opts);

                                    getActivity().finish();
                                    return;
                                }
                            }
                        });
                        builder.show();
                    }
                });

        }



        @Override
        public int getItemCount() {
            return mDataList.size();
        }

    }
}
