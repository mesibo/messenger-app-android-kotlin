/** Copyright (c) 2019 Mesibo
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
 * https://github.com/mesibo/messengerKotlin-app-android
 *
 */

package org.mesibo.messenger

import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.support.annotation.RequiresApi
import android.support.v4.app.FragmentActivity
import android.support.v7.app.AlertDialog
import android.text.Html
import android.text.format.DateFormat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast

import com.mesibo.api.Mesibo
import com.mesibo.calls.MesiboCall
import com.mesibo.messaging.MesiboMessagingFragment
import com.mesibo.messaging.MesiboRecycleViewHolder
import org.mesibo.messenger.Utils.AppUtils

import org.json.JSONException
import org.json.JSONObject

import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.util.ArrayList
import java.util.Calendar
import java.util.Locale
import java.util.Objects

import android.view.View.GONE
import android.view.View.VISIBLE


 class MessagingUIFragmentNew:MesiboMessagingFragment(), MesiboRecycleViewHolder.Listener, Mesibo.FileTransferListener {

internal var doubleBackToExitPressedOnce = false
 var mTYPE = 0
private var mProgressBar:ProgressBar? = null
internal var mMid:Long = 0
internal lateinit var mMesiboMessage:Mesibo.MesiboMessage
internal var mMesiborecyclerView:MesiboRecycleViewHolder? = null
internal var mMessageTypeIncoming:Boolean = false
internal var mMessageSelected = false
internal var mesiboRecycleViewHolderArrayList = ArrayList<MesiboRecycleViewHolder>()
internal var mMidToDeleteList = ArrayList<Long>()
internal var replyEnabled = false
internal var mHideRepy = 1
private val mUser:Mesibo.UserProfile? = null

internal var mRecyclerPosArrayList = ArrayList<Int>()
private val images = arrayOf<Int>(R.drawable.ic_av_timer_black_18dp, R.drawable.message_got_receipt_from_server, R.drawable.message_got_receipt_from_target, R.drawable.message_got_read_receipt_from_target, R.drawable.ic_action_block)
private val mAudioMissedImage = R.drawable.baseline_call_missed_black_24
private val mVideoMissedImage = R.drawable.baseline_missed_video_call_black_24

internal var mBlackTintColor = Color.parseColor("#858586")


public override fun Mesibo_onGetItemViewType(messageParams:Mesibo.MessageParams, s:String):Int {


if (messageParams.isIncoming())
{
return MesiboRecycleViewHolder.TYPE_INCOMING
}

if (messageParams.getStatus() == MesiboRecycleViewHolder.TYPE_HEADER)
{
return MesiboRecycleViewHolder.TYPE_HEADER
}

if (messageParams.isSavedMessage())
{
return MesiboRecycleViewHolder.TYPE_CUSTOM
}

if (messageParams.isMissedCall())
{
return MesiboRecycleViewHolder.TYPE_MISSEDCALL
}

return MesiboRecycleViewHolder.TYPE_OUTGOING


 // return 0;
    }


inner class HeaderViewHolder(internal var mViewHeader:View):MesiboRecycleViewHolder(mViewHeader)

inner class SendContactViewHolder(internal var mViewSendContact:View):MesiboRecycleViewHolder(mViewSendContact) {

internal var mContactName:TextView
internal var mMessageContact:TextView
internal var mContactImage:ImageView

init{
mContactName = mViewSendContact.findViewById<TextView>(R.id.contactName)
mMessageContact = mViewSendContact.findViewById<TextView>(R.id.messageContact)
mContactImage = mViewSendContact.findViewById<ImageView>(R.id.contactImage)


}
}


inner class ReceiveContactViewHolder(internal var mViewReceiveContact:View):MesiboRecycleViewHolder(mViewReceiveContact) {

internal var mContactName:TextView
internal var mMessageContact:TextView
internal var mAddToContact:TextView
internal var mContactImage:ImageView

init{
mContactName = mViewReceiveContact.findViewById<TextView>(R.id.contactName)
mMessageContact = mViewReceiveContact.findViewById<TextView>(R.id.messageTV)
mAddToContact = mViewReceiveContact.findViewById<TextView>(R.id.addToContactTV)
mContactImage = mViewReceiveContact.findViewById<ImageView>(R.id.contactImage)


}
}


inner class MissedCallViewHolder(internal var mMissedViewHeader:View):MesiboRecycleViewHolder(mMissedViewHeader) {

internal var missedTS:TextView
internal var callTypeText:TextView
internal var customMessageTV:TextView
internal var missedCallImage:ImageView
internal var mMissedCallLayout:LinearLayout
internal var mCustomMessageLayout:LinearLayout

init{


mMissedCallLayout = mMissedViewHeader.findViewById<LinearLayout>(R.id.missedCallLayout)
mCustomMessageLayout = mMissedViewHeader.findViewById<LinearLayout>(R.id.customeMessageLayout)
customMessageTV = mMissedViewHeader.findViewById<TextView>(R.id.customeMessage)
missedTS = mMissedViewHeader.findViewById<TextView>(R.id.missedcallTS)
callTypeText = mMissedViewHeader.findViewById<TextView>(R.id.callTypeMsg)
missedCallImage = mMissedViewHeader.findViewById<ImageView>(R.id.missedCallImage)


}
}

@RequiresApi(Build.VERSION_CODES.M)
inner class IncomingMessgaeViewHolder(internal var mViewIncomingMessage:View):MesiboRecycleViewHolder(mViewIncomingMessage) {


internal var mRootLayout:RelativeLayout
internal var mAudioLayout:RelativeLayout
internal var mOthersLayout:RelativeLayout
internal var mProgressLayout:RelativeLayout
internal var mPlayButtonLayout:RelativeLayout
internal var mMessageLayout:LinearLayout
internal var mCaptionTv:TextView
internal var mAudioduration:TextView
internal var mSenderName:TextView
internal var mMessageTV:TextView
internal var mChatTimeTv:TextView
internal var mAudioPlayButton:ImageView
internal var mAudioPauseButton:ImageView
internal var mImageContainerView:ImageView
internal var mPlayButton:ImageView
internal var mMediaProgress:ProgressBar
internal var mLinkPreviewLayout:LinearLayout
internal var mLinkImageLayout:LinearLayout
internal var mLinkCancelLayout:LinearLayout
internal var mLinkImage:ImageView
internal var mLinkTitle:TextView
internal var mLinkDesc:TextView
internal var mLinkUrl:TextView
internal var mLinkActive = true


internal var mLiked:Boolean? = false

init{
mRootLayout = mViewIncomingMessage.findViewById<RelativeLayout>(R.id.rootLayout)
mMessageLayout = mViewIncomingMessage.findViewById<LinearLayout>(R.id.messageLayout)
mAudioLayout = mViewIncomingMessage.findViewById<RelativeLayout>(R.id.audioLayout)
mOthersLayout = mViewIncomingMessage.findViewById<RelativeLayout>(R.id.otherLayout)
mProgressLayout = mViewIncomingMessage.findViewById<RelativeLayout>(R.id.progressLayout)
mPlayButtonLayout = mViewIncomingMessage.findViewById<RelativeLayout>(R.id.playButtonLayout)
mCaptionTv = mViewIncomingMessage.findViewById<TextView>(R.id.captionTV)
mAudioduration = mViewIncomingMessage.findViewById<TextView>(R.id.audioDuration)
mSenderName = mViewIncomingMessage.findViewById<TextView>(R.id.senderName)
mMessageTV = mViewIncomingMessage.findViewById<TextView>(R.id.messageText)
mChatTimeTv = mViewIncomingMessage.findViewById<TextView>(R.id.chatTimeTv)
mPlayButton = mViewIncomingMessage.findViewById<ImageView>(R.id.mediaPlayButton)
mAudioPlayButton = mViewIncomingMessage.findViewById<ImageView>(R.id.audioPlayButton)
mAudioPauseButton = mViewIncomingMessage.findViewById<ImageView>(R.id.audioPauseButton)
mImageContainerView = mViewIncomingMessage.findViewById<ImageView>(R.id.ImageContainerView)
mMediaProgress = mViewIncomingMessage.findViewById<ProgressBar>(R.id.progressCircularBar)

mChatTimeTv.setTextAppearance(R.style.chat_timings_params)

mLinkPreviewLayout = mViewIncomingMessage.findViewById<LinearLayout>(R.id.linkPreviewLayout)
mLinkImageLayout = mViewIncomingMessage.findViewById<LinearLayout>(R.id.linkImageLayout)
mLinkCancelLayout = mViewIncomingMessage.findViewById<LinearLayout>(R.id.linkPreviewCancel)
mLinkTitle = mViewIncomingMessage.findViewById<TextView>(R.id.linkTitle)
mLinkDesc = mViewIncomingMessage.findViewById<TextView>(R.id.linkDescription)
mLinkUrl = mViewIncomingMessage.findViewById<TextView>(R.id.linkUrl)
mLinkImage = mViewIncomingMessage.findViewById<ImageView>(R.id.linkImage)
mLinkCancelLayout.setVisibility(GONE)
mLinkImageLayout.setVisibility(GONE)

mLinkCancelLayout.setOnClickListener(object:View.OnClickListener {
public override fun onClick(v:View) {
mLinkPreviewLayout.setVisibility(GONE)
}
})

mLinkPreviewLayout.setOnClickListener(object:View.OnClickListener {
public override fun onClick(v:View) {
val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(mLinkUrl.getText().toString().trim({ it <= ' ' })))
startActivity(browserIntent)
}
})
}
}


@RequiresApi(Build.VERSION_CODES.M)
inner class OutgoingMessgaeViewHolder(internal var mViewOutgoingMessage:View):MesiboRecycleViewHolder(mViewOutgoingMessage) {

internal var mRootLayout:RelativeLayout
internal var mAudioLayout:RelativeLayout
internal var mOthersLayout:RelativeLayout
internal var mProgressLayout:RelativeLayout
internal var mPlayButtonLayout:RelativeLayout
internal var mMessageLayout:LinearLayout
internal var mCaptionTv:TextView
internal var mAudioduration:TextView
internal var mMessageTV:TextView
internal var mChatTimeTv:TextView
internal var mAudioPlayButton:ImageView
internal var mAudioPauseButton:ImageView
internal var mImageContainerView:ImageView
internal var mPlayButton:ImageView
internal var mOutMsgStatusTickIV:ImageView
internal var mMediaProgress:ProgressBar
internal var mLinkPreviewLayout:LinearLayout
internal var mLinkImageLayout:LinearLayout
internal var mLinkCancelLayout:LinearLayout
internal var mLinkImage:ImageView
internal var mLinkTitle:TextView
internal var mLinkDesc:TextView
internal var mLinkUrl:TextView
internal var mLinkActive = true


internal var mLiked:Boolean? = false

init{

mRootLayout = mViewOutgoingMessage.findViewById<RelativeLayout>(R.id.rootLayout)
mMessageLayout = mViewOutgoingMessage.findViewById<LinearLayout>(R.id.messageLayout)
mAudioLayout = mViewOutgoingMessage.findViewById<RelativeLayout>(R.id.audioLayout)
mOthersLayout = mViewOutgoingMessage.findViewById<RelativeLayout>(R.id.otherLayout)
mProgressLayout = mViewOutgoingMessage.findViewById<RelativeLayout>(R.id.progressLayout)
mPlayButtonLayout = mViewOutgoingMessage.findViewById<RelativeLayout>(R.id.playButtonLayout)
mCaptionTv = mViewOutgoingMessage.findViewById<TextView>(R.id.captionTV)
mAudioduration = mViewOutgoingMessage.findViewById<TextView>(R.id.audioDuration)
mMessageTV = mViewOutgoingMessage.findViewById<TextView>(R.id.messageText)
mChatTimeTv = mViewOutgoingMessage.findViewById<TextView>(R.id.chatTimeTv)
mPlayButton = mViewOutgoingMessage.findViewById<ImageView>(R.id.mediaPlayButton)
mAudioPlayButton = mViewOutgoingMessage.findViewById<ImageView>(R.id.audioPlayButton)
mAudioPauseButton = mViewOutgoingMessage.findViewById<ImageView>(R.id.audioPauseButton)
mImageContainerView = mViewOutgoingMessage.findViewById<ImageView>(R.id.ImageContainerView)
mMediaProgress = mViewOutgoingMessage.findViewById<ProgressBar>(R.id.progressCircularBar)
mOutMsgStatusTickIV = mViewOutgoingMessage.findViewById<ImageView>(R.id.outMsgStatusIV)

mChatTimeTv.setTextAppearance(R.style.chat_timings_params_outgoing)


mLinkPreviewLayout = mViewOutgoingMessage.findViewById<LinearLayout>(R.id.linkPreviewLayout)
mLinkImageLayout = mViewOutgoingMessage.findViewById<LinearLayout>(R.id.linkImageLayout)
mLinkCancelLayout = mViewOutgoingMessage.findViewById<LinearLayout>(R.id.linkPreviewCancel)
mLinkTitle = mViewOutgoingMessage.findViewById<TextView>(R.id.linkTitle)
mLinkDesc = mViewOutgoingMessage.findViewById<TextView>(R.id.linkDescription)
mLinkUrl = mViewOutgoingMessage.findViewById<TextView>(R.id.linkUrl)
mLinkImage = mViewOutgoingMessage.findViewById<ImageView>(R.id.linkImage)
mLinkCancelLayout.setVisibility(GONE)
mLinkImageLayout.setVisibility(GONE)

mLinkCancelLayout.setOnClickListener(object:View.OnClickListener {
public override fun onClick(v:View) {
mLinkPreviewLayout.setVisibility(GONE)
}
})

mLinkPreviewLayout.setOnClickListener(object:View.OnClickListener {
public override fun onClick(v:View) {
val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(mLinkUrl.getText().toString().trim({ it <= ' ' })))
startActivity(browserIntent)
}
})

}
}


@RequiresApi(Build.VERSION_CODES.M)
public override fun Mesibo_onCreateViewHolder(viewGroup:ViewGroup, type:Int):MesiboRecycleViewHolder? {


if (TYPE_SEND_CONATCT == type)
{


val v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.send_contact_row_item, viewGroup, false)
return SendContactViewHolder(v)

}
else if (TYPE_RECEIVE_CONTACT == type)
{

val v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.receive_contact_row_item, viewGroup, false)
return ReceiveContactViewHolder(v)

}
else if (MesiboRecycleViewHolder.TYPE_HEADER == type)
{

val v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.chat_header_view, viewGroup, false)
return HeaderViewHolder(v)

}
else if (MesiboRecycleViewHolder.TYPE_MISSEDCALL == type)
{

val v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.chat_missedcall_view, viewGroup, false)
return MissedCallViewHolder(v)

}
else if (MesiboRecycleViewHolder.TYPE_OUTGOING == type)
{

 //            if (mDefaultLayout)
 //
 //                return null;

            val v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.outgoing_chat_layout, viewGroup, false)
return OutgoingMessgaeViewHolder(v)

}
else if (MesiboRecycleViewHolder.TYPE_INCOMING == type)
{

 //            if (mDefaultLayout)
 //
 //                return null;

            val v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.incoming_chat_layout, viewGroup, false)
return IncomingMessgaeViewHolder(v)

}


return null
}


public override fun Mesibo_onBindViewHolder(mesiboRecycleViewHolder:MesiboRecycleViewHolder, type:Int, b:Boolean, messageParams:Mesibo.MessageParams?, mesiboMessage:Mesibo.MesiboMessage?) {

    //Brute fix
if(null == mesiboMessage)
    return


if (type == MesiboRecycleViewHolder.TYPE_HEADER)
{

mTYPE = MesiboRecycleViewHolder.TYPE_HEADER
val HeaderView = mesiboRecycleViewHolder as HeaderViewHolder

}
else if (type == MesiboRecycleViewHolder.TYPE_MISSEDCALL)
{
mTYPE = type
val missedCallViewHolder = mesiboRecycleViewHolder as MissedCallViewHolder
missedCallViewHolder.mCustomMessageLayout.setVisibility(View.GONE)
missedCallViewHolder.mMissedCallLayout.setVisibility(View.VISIBLE)
missedCallViewHolder.missedTS.setText(getTIME(mesiboMessage.ts))

if (messageParams!!.isVoiceCall())
{
missedCallViewHolder.callTypeText.setText("Missed audio call at")
missedCallViewHolder.missedCallImage.setImageResource(mAudioMissedImage)
}
else
{
missedCallViewHolder.callTypeText.setText("Missed video call at")
missedCallViewHolder.missedCallImage.setImageResource(mVideoMissedImage)
}

}
else if (type == MesiboRecycleViewHolder.TYPE_CUSTOM)
{

val customMessage = mesiboRecycleViewHolder as MissedCallViewHolder
customMessage.mCustomMessageLayout.setVisibility(View.VISIBLE)
customMessage.mMissedCallLayout.setVisibility(View.GONE)
customMessage.customMessageTV.setText(mesiboMessage?.message)

}
else if (type == MesiboRecycleViewHolder.TYPE_INCOMING)
{

mTYPE = MesiboRecycleViewHolder.TYPE_INCOMING
val IncomingView = mesiboRecycleViewHolder as IncomingMessgaeViewHolder

if (messageParams!!.isDeleted())
{

val time = getTIME(mesiboMessage.ts)
IncomingView.mLinkPreviewLayout.setVisibility(GONE)
IncomingView.mMessageTV.setText(time)
IncomingView.mMessageTV.setTextColor(Color.LTGRAY)
IncomingView.mMessageTV.setText(Html.fromHtml(("&#216 " + "This message was deleted"
+ " &#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;")))

}
else
{

val time = getTIME(mesiboMessage.ts)
IncomingView.mChatTimeTv.setText(time)
val respondersProfile = messageParams?.profile

if (null == messageParams?.groupProfile && 0L == messageParams.groupid)
{
IncomingView.mSenderName.setVisibility(View.GONE)
}
else
{
val nameM = respondersProfile.name
IncomingView.mSenderName.setText(nameM)
}


if (null != mesiboMessage.file  /*|| null != mesiboMessage.location*/)
{
IncomingView.mLinkPreviewLayout.setVisibility(GONE)
IncomingView.mAudioLayout.setVisibility(View.GONE)
IncomingView.mProgressLayout.setVisibility(View.VISIBLE)
IncomingView.mMessageLayout.setVisibility(View.GONE)

mProgressBar = IncomingView.mMediaProgress

if (mesiboMessage.file.isTransferred())
{
IncomingView.mProgressLayout.setVisibility(View.GONE)
}

 //Check group message, if yes show senders name
                    if (null != messageParams.groupProfile || 0 < messageParams.groupid)
{
IncomingView.mSenderName.setVisibility(View.VISIBLE)
}

 //Check for caption , if there make caption visible
                    if (!mesiboMessage.message.trim({ it <= ' ' }).isEmpty())
{
IncomingView.mCaptionTv.setVisibility(View.VISIBLE)
IncomingView.mCaptionTv.setText(mesiboMessage.message)
}


 //Image or Video
                    if (mesiboMessage.file.type == Mesibo.FileInfo.TYPE_IMAGE || mesiboMessage.file.type == Mesibo.FileInfo.TYPE_VIDEO /*|| null != mesiboMessage.location*/)
{
IncomingView.mAudioLayout.setVisibility(View.GONE)
IncomingView.mOthersLayout.setVisibility(View.VISIBLE)
val imgFile = File(mesiboMessage.file.getPath())

 // set Image
                        IncomingView.mImageContainerView.setImageBitmap(mesiboMessage.file.image)


if (mesiboMessage.file.type == Mesibo.FileInfo.TYPE_IMAGE)
{
IncomingView.mImageContainerView.setOnClickListener(object:View.OnClickListener {
public override fun onClick(v:View) {
UIManager.launchImageViewer(getActivity()!!, mesiboMessage.file.getPath())

}
})
}
 //set play button for Video
                        if (mesiboMessage.file.type == Mesibo.FileInfo.TYPE_VIDEO)
{

IncomingView.mPlayButtonLayout.setVisibility(View.VISIBLE)
IncomingView.mPlayButton.setOnClickListener(object:View.OnClickListener {
public override fun onClick(view:View) {


val file = File(mesiboMessage.file.getPath())

 //                                    Intent intent = new Intent(Intent.ACTION_VIEW);
 //                                    intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
 //                                    intent.setDataAndType(Uri.parse(mesiboMessage.file.getUrl()), "video/mp4");
 //                                    startActivity(intent);
                                    UIManager.openMedia(getActivity()!!, mesiboMessage.file.getUrl(), mesiboMessage.file.getPath())

}
})


}
else
{
IncomingView.mPlayButtonLayout.setVisibility(View.GONE)
}

 // location

 //                        if (null != mesiboMessage.location) {
 //
 //
 //
 //
 //                            IncomingView.mImageContainerView.setOnClickListener(new View.OnClickListener() {
 //                                @Override
 //                                public void onClick(View view) {
 //
 //                                }
 //                            });
 //
 //
 //                        }


                    }


 //Audio

                    if (mesiboMessage.file.type == Mesibo.FileInfo.TYPE_AUDIO)
{


IncomingView.mAudioLayout.setVisibility(View.VISIBLE)
IncomingView.mOthersLayout.setVisibility(View.GONE)
IncomingView.mCaptionTv.setVisibility(View.GONE)
IncomingView.mPlayButtonLayout.setVisibility(View.GONE)
IncomingView.mMessageLayout.setVisibility(View.GONE)

if (mesiboMessage.file.isTransferred())
{

IncomingView.mProgressLayout.setVisibility(View.GONE)
IncomingView.mAudioPlayButton.setVisibility(View.VISIBLE)

}
else
{

IncomingView.mProgressLayout.setVisibility(View.VISIBLE)
IncomingView.mAudioPlayButton.setVisibility(View.GONE)
}

 //set up MediaPlayer
                        val mp = MediaPlayer()
try
{
val path = mesiboMessage.file.getPath()
mp.setDataSource(path)
mp.prepare()

val duration = mp.getDuration().toFloat()
val durationLong = (duration / 60000).toDouble()
IncomingView.mAudioduration.setText(String.format("%.2f", durationLong))
}
catch (e:Exception) {
e.printStackTrace()
}


IncomingView.mAudioPlayButton.setOnClickListener(object:View.OnClickListener {
public override fun onClick(view:View) {
UIManager.openMedia(getActivity()!!, mesiboMessage.file.getUrl(), mesiboMessage.file.getPath())
 //                                IncomingView.mAudioPlayButton.setVisibility(View.GONE);
 //                                IncomingView.mAudioPauseButton.setVisibility(View.VISIBLE);
 //                                mp.start();

                            }
})

IncomingView.mAudioPauseButton.setOnClickListener(object:View.OnClickListener {
public override fun onClick(view:View) {
IncomingView.mAudioPlayButton.setVisibility(View.VISIBLE)
IncomingView.mAudioPauseButton.setVisibility(View.GONE)
mp.pause()
}
})


}

if (mesiboMessage.file.type == Mesibo.FileInfo.TYPE_OTHER)
{
IncomingView.mAudioLayout.setVisibility(View.VISIBLE)
IncomingView.mOthersLayout.setVisibility(View.GONE)
IncomingView.mCaptionTv.setVisibility(View.GONE)
IncomingView.mPlayButtonLayout.setVisibility(View.GONE)
IncomingView.mMessageLayout.setVisibility(View.GONE)

 // set Image
                        IncomingView.mImageContainerView.setImageBitmap(mesiboMessage.file.image)

IncomingView.mImageContainerView.setOnClickListener(object:View.OnClickListener {
public override fun onClick(v:View) {
UIManager.openMedia(getActivity()!!, mesiboMessage.file.getUrl(), mesiboMessage.file.getPath())
}
})
}

}
else
{

IncomingView.mLinkPreviewLayout.setVisibility(GONE)
IncomingView.mAudioLayout.setVisibility(View.GONE)
IncomingView.mOthersLayout.setVisibility(View.GONE)
IncomingView.mCaptionTv.setVisibility(View.GONE)
IncomingView.mProgressLayout.setVisibility(View.GONE)
IncomingView.mPlayButtonLayout.setVisibility(View.GONE)
IncomingView.mMessageLayout.setVisibility(View.VISIBLE)

if (null != messageParams.groupProfile && 0 < messageParams.groupid)
{
IncomingView.mSenderName.setVisibility(View.VISIBLE)
}


IncomingView.mMessageTV.setText(Html.fromHtml((mesiboMessage.message + " &#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;")))

val linksInText = AppUtils.extractLinks(mesiboMessage.message)

if (linksInText.size >= 1)
{
 // Toast.makeText(ActivityPostCreate.this, "" + linksInText[0], Toast.LENGTH_SHORT).show();


                        if (linksInText[0].contains(".com") && IncomingView.mLinkActive)
{

SampleAPI.getLinkPreview(linksInText[0], object:SampleAPI.ApiResponseHandler {
public override fun onApiResponse(result:Boolean, response:String?) {

if (result)
{

try
{
val jObject = JSONObject(response)


val title = jObject.getString("title")
val description = jObject.getString("description")
val image = jObject.getString("image")
val url = jObject.getString("url")

if (null != title && !title!!.isEmpty())
{
IncomingView.mLinkActive = true

IncomingView.mLinkTitle.setText(title)
IncomingView.mLinkDesc.setText(description)
IncomingView.mLinkUrl.setText(url)

if (null != image && !image!!.isEmpty())
{

IncomingView.mLinkImageLayout.setVisibility(VISIBLE)
val mTempFilePath = Mesibo.getFilePath(Mesibo.FileInfo.TYPE_PROFILETHUMBNAIL) + title + "_image.jpg"

val imgFile1 = File(mTempFilePath)
if (imgFile1.exists())
{
val myBitmap = BitmapFactory.decodeFile(imgFile1.getAbsolutePath())
IncomingView.mLinkImage.setImageBitmap(myBitmap)
}
else
{
downloadFile(image, IncomingView.mLinkImage, mTempFilePath)
}

}
else
{
IncomingView.mLinkImageLayout.setVisibility(GONE)
}

IncomingView.mLinkPreviewLayout.setVisibility(VISIBLE)


}
else
{
IncomingView.mLinkActive = false
}


}
catch (e:JSONException) {
e.printStackTrace()
}


}

}

})
}
else
{
IncomingView.mLinkPreviewLayout.setVisibility(GONE)
}

}




}


}

IncomingView.itemView.setOnClickListener(object:View.OnClickListener {
public override fun onClick(v:View) {

val pos = IncomingView.getAdapterPosition()


mMessageTypeIncoming = true

if (mRecyclerPosArrayList.size > 0)
{
if (!mRecyclerPosArrayList.contains(pos))
{
mRecyclerPosArrayList.add(pos)
mMidToDeleteList.add(mMid)
mesiboRecycleViewHolderArrayList.add(IncomingView)
IncomingView.mRootLayout.setBackgroundColor(AppConfig.Grey_color)
}
else
{
mRecyclerPosArrayList.remove(Integer.valueOf(pos))
mMidToDeleteList.remove(mMid)
mesiboRecycleViewHolderArrayList.remove(IncomingView)
IncomingView.mRootLayout.setBackgroundColor(AppConfig.Transparent)

if (mRecyclerPosArrayList.size == 0)
{
(Objects.requireNonNull<FragmentActivity>(getActivity()) as MessagingActivityNew).Mesibo_onHideInContextUserInterface()
mHideRepy = 1

}
}

}

if (mRecyclerPosArrayList.size > 1)
{

mHideRepy = 0
(Objects.requireNonNull<FragmentActivity>(getActivity()) as MessagingActivityNew).Mesibo_onShowInContextUserInterface()
(Objects.requireNonNull<FragmentActivity>(getActivity()) as MessagingActivityNew).Mesibo_onContextUserInterfaceCount(mRecyclerPosArrayList.size)
}


}
})

IncomingView.itemView.setOnLongClickListener(object:View.OnLongClickListener {
public override fun onLongClick(view:View):Boolean {

mMessageSelected = true

val pos = IncomingView.getAdapterPosition()


mMid = mesiboMessage.mid


if (!mRecyclerPosArrayList.contains(pos))
{
mRecyclerPosArrayList.add(pos)
IncomingView.mRootLayout.setBackgroundColor(AppConfig.Grey_color)
mMidToDeleteList.add(mMid)
mesiboRecycleViewHolderArrayList.add(IncomingView)
}
else
{
mRecyclerPosArrayList.remove(Integer.valueOf(pos))
mMidToDeleteList.remove(mMid)
mesiboRecycleViewHolderArrayList.remove(IncomingView)
IncomingView.mRootLayout.setBackgroundColor(AppConfig.Transparent)
if (mRecyclerPosArrayList.size == 0)
{
(Objects.requireNonNull<FragmentActivity>(getActivity()) as MessagingActivityNew).Mesibo_onHideInContextUserInterface()
mHideRepy = 1
}
}

if (mRecyclerPosArrayList.size > 1)
{
(Objects.requireNonNull<FragmentActivity>(getActivity()) as MessagingActivityNew).Mesibo_onContextUserInterfaceCount(mRecyclerPosArrayList.size)
mHideRepy = 0
}


 //                        mMessageSelected = !mMessageSelected;
 //
 //                        if(mMessageSelected){
                    IncomingView.mRootLayout.setBackgroundColor(AppConfig.Grey_color)
mMesiboMessage = mesiboMessage
mMesiborecyclerView = IncomingView
mMessageTypeIncoming = true
 // mMid = mesiboMessage.mid;
                    //mMidToDeleteList.add(mMid);
                    //mesiboRecycleViewHolderArrayList.add(IncomingView);


                    (Objects.requireNonNull<FragmentActivity>(getActivity()) as MessagingActivityNew).Mesibo_onShowInContextUserInterface()
 //                        }else{
 //                            IncomingView.mRootLayout.setBackgroundColor(AppConfig.Transparent);
 //                            ((MessagingActivityNew) Objects.requireNonNull(getActivity())).Mesibo_onHideInContextUserInterface();
 //                        }


                    return true
}
})


IncomingView.mSenderName.setOnClickListener(object:View.OnClickListener {
public override fun onClick(view:View) {

 // setup the alert builder
                    val builder = AlertDialog.Builder(getActivity()!!)

val actions = arrayOf<String>("Message", "Video Call", "Audio Call")
builder.setItems(actions, object:DialogInterface.OnClickListener {
public override fun onClick(dialog:DialogInterface, which:Int) {
when (which) {
0 // Message
 -> {

val i = Intent(getActivity(), MessagingActivityNew::class.java)
i.putExtra("peer", messageParams.peer)
startActivity(i)
dialog.dismiss()
}

1 // Video Call
 -> {
val mParameter = Mesibo.MessageParams(messageParams.peer, 0, Mesibo.FLAG_DEFAULT, 0)
MesiboCall.getInstance().call(getActivity(), Mesibo.random(), mParameter.profile, true)
dialog.dismiss()
}
2 // Audio Call
 -> {
val mParameter1 = Mesibo.MessageParams(messageParams.peer, 0, Mesibo.FLAG_DEFAULT, 0)
MesiboCall.getInstance().call(getActivity(), Mesibo.random(), mParameter1.profile, false)
dialog.dismiss()
}
}
}
})

val dialog = builder.create()
dialog.show()
}
})


}
else if (type == MesiboRecycleViewHolder.TYPE_OUTGOING)
{

mTYPE = MesiboRecycleViewHolder.TYPE_OUTGOING


val OutView = mesiboRecycleViewHolder as OutgoingMessgaeViewHolder
val time = getTIME(mesiboMessage.ts)

if (messageParams!!.isDeleted())
{
OutView.mLinkPreviewLayout.setVisibility(GONE)

OutView.mMessageTV.setText(time)
OutView.mMessageTV.setTextColor(Color.LTGRAY)
OutView.mMessageTV.setText(Html.fromHtml(("&#216 " + "You deleted this message."
+ " &#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;" +
"&#160;&#160;&#160;&#160;&#160;&#160;&#160;")))
OutView.mOutMsgStatusTickIV.setVisibility(View.GONE)


}
else
{
val ts = getTIME(mesiboMessage.ts)
OutView.mChatTimeTv.setText(ts)
val respondersProfile = messageParams?.profile


if (null != mesiboMessage.file  /*|| null != mesiboMessage.location*/)
{

OutView.mLinkPreviewLayout.setVisibility(GONE)
OutView.mProgressLayout.setVisibility(View.VISIBLE)
OutView.mMessageLayout.setVisibility(View.GONE)


mProgressBar = OutView.mMediaProgress

if (mesiboMessage.file.isTransferred())
{
OutView.mProgressLayout.setVisibility(View.GONE)
}

 //Check for caption , if there make caption visible
                    if (!mesiboMessage.message.trim({ it <= ' ' }).isEmpty())
{
OutView.mCaptionTv.setVisibility(View.VISIBLE)
OutView.mCaptionTv.setText(mesiboMessage.message)
}


 //Image or Video
                    if (mesiboMessage.file.type == Mesibo.FileInfo.TYPE_IMAGE || mesiboMessage.file.type == Mesibo.FileInfo.TYPE_VIDEO /*|| null != mesiboMessage.location*/)
{
OutView.mAudioLayout.setVisibility(View.GONE)
OutView.mOthersLayout.setVisibility(View.VISIBLE)
val imgFile = File(mesiboMessage.file.getPath())

 // set Image
                        OutView.mImageContainerView.setImageBitmap(mesiboMessage.file.image)
 //                        if (imgFile.exists()) {
 //                            Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
 //                            OutView.mImageContainerView.setImageBitmap(myBitmap);
 //                        }


                        if (mesiboMessage.file.type == Mesibo.FileInfo.TYPE_IMAGE)
{
OutView.mImageContainerView.setOnClickListener(object:View.OnClickListener {
public override fun onClick(v:View) {
UIManager.launchImageViewer(getActivity()!!, mesiboMessage.file.getPath())

}
})
}
 //set play button for Video

                        if (mesiboMessage.file.type == Mesibo.FileInfo.TYPE_VIDEO)
{

OutView.mPlayButtonLayout.setVisibility(View.VISIBLE)
OutView.mPlayButton.setOnClickListener(object:View.OnClickListener {
public override fun onClick(view:View) {


val file = File(mesiboMessage.file.getPath())

 //                                    Intent intent = new Intent(Intent.ACTION_VIEW);
 //                                    intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
 //                                    intent.setDataAndType(Uri.parse(mesiboMessage.file.getUrl()), "video/mp4");
 //                                    startActivity(intent);


 //                                    if(Mesibo.fileExists(mesiboMessage.file.getPath())){
 //                                        MimeTypeMap myMime = MimeTypeMap.getSingleton();
 //                                        Intent newIntent = new Intent(Intent.ACTION_VIEW);
 //                                        String mimeType = myMime.getMimeTypeFromExtension(fileExt(mesiboMessage.file.getUrl()).substring(1));
 //                                        newIntent.setDataAndType(Uri.fromFile(file),mimeType);
 //                                        newIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
 //                                        try {
 //                                            getActivity().startActivity(newIntent);
 //                                        } catch (ActivityNotFoundException e) {
 //                                            Toast.makeText(getActivity(), "No handler for this type of file.", Toast.LENGTH_LONG).show();
 //                                        }
 //
 //                                    }else {

                                       UIManager.openMedia(getActivity()!!, mesiboMessage.file.getUrl(), mesiboMessage.file.getPath())

 // }

                                }
})




}
else
{
OutView.mPlayButtonLayout.setVisibility(View.GONE)
}

 // location

 //                        if (null != mesiboMessage.location) {
 //
 //
 //
 //
 //                            IncomingView.mImageContainerView.setOnClickListener(new View.OnClickListener() {
 //                                @Override
 //                                public void onClick(View view) {
 //
 //                                }
 //                            });
 //
 //
 //                        }


                    }


 //Audio

                    if (mesiboMessage.file.type == Mesibo.FileInfo.TYPE_AUDIO)
{

OutView.mAudioLayout.setVisibility(View.VISIBLE)
OutView.mOthersLayout.setVisibility(View.GONE)
OutView.mCaptionTv.setVisibility(View.GONE)
OutView.mPlayButtonLayout.setVisibility(View.GONE)
OutView.mMessageLayout.setVisibility(View.GONE)


if (mesiboMessage.file.isTransferred())
{

OutView.mProgressLayout.setVisibility(View.GONE)
OutView.mAudioPlayButton.setVisibility(View.VISIBLE)

}
else
{

OutView.mProgressLayout.setVisibility(View.VISIBLE)
OutView.mAudioPlayButton.setVisibility(View.GONE)
}

 //set up MediaPlayer
                        val mp = MediaPlayer()
try
{
val path = mesiboMessage.file.getPath()
mp.setDataSource(path)
mp.prepare()

val duration = mp.getDuration().toFloat()
val durationLong = (duration / 60000).toDouble()
OutView.mAudioduration.setText(String.format("%.2f", durationLong))
}
catch (e:Exception) {
e.printStackTrace()
}


OutView.mAudioPlayButton.setOnClickListener(object:View.OnClickListener {
public override fun onClick(view:View) {
 //                                OutView.mAudioPlayButton.setVisibility(View.GONE);
 //                                OutView.mAudioPauseButton.setVisibility(View.VISIBLE);
 //                                mp.start();

                                UIManager.openMedia(getActivity()!!, mesiboMessage.file.getUrl(), mesiboMessage.file.getPath())

}
})

OutView.mAudioPauseButton.setOnClickListener(object:View.OnClickListener {
public override fun onClick(view:View) {
OutView.mAudioPlayButton.setVisibility(View.VISIBLE)
OutView.mAudioPauseButton.setVisibility(View.GONE)
mp.pause()
}
})


}


if (mesiboMessage.file.type == Mesibo.FileInfo.TYPE_OTHER)
{
OutView.mAudioLayout.setVisibility(View.GONE)
OutView.mOthersLayout.setVisibility(View.VISIBLE)
val imgFile = File(mesiboMessage.file.getPath())

 // set Image
                        OutView.mImageContainerView.setImageBitmap(mesiboMessage.file.image)

OutView.mImageContainerView.setOnClickListener(object:View.OnClickListener {
public override fun onClick(v:View) {
UIManager.openMedia(getActivity()!!, mesiboMessage.file.getUrl(), mesiboMessage.file.getPath())
}
})
}

}
else
{// message

OutView.mLinkPreviewLayout.setVisibility(GONE)
OutView.mAudioLayout.setVisibility(View.GONE)
OutView.mOthersLayout.setVisibility(View.GONE)
OutView.mCaptionTv.setVisibility(View.GONE)
OutView.mProgressLayout.setVisibility(View.GONE)
OutView.mPlayButtonLayout.setVisibility(View.GONE)
OutView.mMessageLayout.setVisibility(View.VISIBLE)


OutView.mMessageTV.setText(Html.fromHtml((mesiboMessage.message
+ " &#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;" +
"&#160;&#160;&#160;&#160;&#160;&#160;&#160;")))


val linksInText = AppUtils.extractLinks(mesiboMessage.message)

if (linksInText.size >= 1)
{
 // Toast.makeText(ActivityPostCreate.this, "" + linksInText[0], Toast.LENGTH_SHORT).show();


                        if (linksInText[0].contains(".com") && OutView.mLinkActive)
{

SampleAPI.getLinkPreview(linksInText[0], object:SampleAPI.ApiResponseHandler {
public override fun onApiResponse(result:Boolean, response:String?) {

if (result)
{

try
{
val jObject = JSONObject(response)
val title = jObject.getString("title")
val description = jObject.getString("description")
val image = jObject.getString("image")
val url = jObject.getString("url")
if (null != title && !title!!.isEmpty())
{
OutView.mLinkActive = true

OutView.mLinkTitle.setText(title)
OutView.mLinkDesc.setText(description)
OutView.mLinkUrl.setText(url)

if (null != image && !image!!.isEmpty())
{

OutView.mLinkImageLayout.setVisibility(VISIBLE)
val mTempFilePath = Mesibo.getFilePath(Mesibo.FileInfo.TYPE_PROFILETHUMBNAIL) + title + "_image.jpg"

val imgFile1 = File(mTempFilePath)
if (imgFile1.exists())
{
val myBitmap = BitmapFactory.decodeFile(imgFile1.getAbsolutePath())
OutView.mLinkImage.setImageBitmap(myBitmap)
}
else
{
downloadFile(image, OutView.mLinkImage, mTempFilePath)
}

}
else
{
OutView.mLinkImageLayout.setVisibility(GONE)
}



OutView.mLinkPreviewLayout.setVisibility(VISIBLE)


}
else
{
OutView.mLinkActive = false
}


}
catch (e:JSONException) {
e.printStackTrace()
}


}

}

})
}
else
{
OutView.mLinkPreviewLayout.setVisibility(GONE)
}

}


}

}

if (mesiboMessage.status == Mesibo.MSGSTATUS_OUTBOX)
{
OutView.mOutMsgStatusTickIV.setImageResource(images[0])


}
else if (mesiboMessage.status == Mesibo.MSGSTATUS_SENT)
{
OutView.mOutMsgStatusTickIV.setImageResource(images[1])

}
else if (mesiboMessage.status == Mesibo.MSGSTATUS_DELIVERED)
{

OutView.mOutMsgStatusTickIV.setImageResource(images[2])

}
else if (mesiboMessage.status == Mesibo.MSGSTATUS_READ)
{
OutView.mOutMsgStatusTickIV.setImageResource(images[3])

}
else if (mesiboMessage.status == Mesibo.MSGSTATUS_BLOCKED)
{




}


OutView.itemView.setOnClickListener(object:View.OnClickListener {
public override fun onClick(v:View) {

val pos = OutView.getAdapterPosition()


mMessageTypeIncoming = true

if (mRecyclerPosArrayList.size > 0)
{
if (!mRecyclerPosArrayList.contains(pos))
{
mRecyclerPosArrayList.add(pos)
mMidToDeleteList.add(mMid)
mesiboRecycleViewHolderArrayList.add(OutView)
OutView.mRootLayout.setBackgroundColor(AppConfig.Grey_color)
}
else
{
mRecyclerPosArrayList.remove(Integer.valueOf(pos))
mMidToDeleteList.remove(mMid)
mesiboRecycleViewHolderArrayList.remove(OutView)
OutView.mRootLayout.setBackgroundColor(AppConfig.Transparent)

if (mRecyclerPosArrayList.size == 0)
{
(Objects.requireNonNull<FragmentActivity>(getActivity()) as MessagingActivityNew).Mesibo_onHideInContextUserInterface()
mHideRepy = 1

}
}

}

if (mRecyclerPosArrayList.size > 1)
{

mHideRepy = 0
(Objects.requireNonNull<FragmentActivity>(getActivity()) as MessagingActivityNew).Mesibo_onShowInContextUserInterface()
(Objects.requireNonNull<FragmentActivity>(getActivity()) as MessagingActivityNew).Mesibo_onContextUserInterfaceCount(mRecyclerPosArrayList.size)
}


}
})

OutView.itemView.setOnLongClickListener(object:View.OnLongClickListener {
public override fun onLongClick(view:View):Boolean {

mMessageSelected = true

val pos = OutView.getAdapterPosition()


mMid = mesiboMessage.mid


if (!mRecyclerPosArrayList.contains(pos))
{
mRecyclerPosArrayList.add(pos)
OutView.mRootLayout.setBackgroundColor(AppConfig.Grey_color)
mMidToDeleteList.add(mMid)
mesiboRecycleViewHolderArrayList.add(OutView)
}
else
{
mRecyclerPosArrayList.remove(Integer.valueOf(pos))
mMidToDeleteList.remove(mMid)
mesiboRecycleViewHolderArrayList.remove(OutView)
OutView.mRootLayout.setBackgroundColor(AppConfig.Transparent)
if (mRecyclerPosArrayList.size == 0)
{
(Objects.requireNonNull<FragmentActivity>(getActivity()) as MessagingActivityNew).Mesibo_onHideInContextUserInterface()
mHideRepy = 1
}
}

if (mRecyclerPosArrayList.size > 1)
{
(Objects.requireNonNull<FragmentActivity>(getActivity()) as MessagingActivityNew).Mesibo_onContextUserInterfaceCount(mRecyclerPosArrayList.size)
mHideRepy = 0
}


 //                        mMessageSelected = !mMessageSelected;
 //
 //                        if(mMessageSelected){
                    OutView.mRootLayout.setBackgroundColor(AppConfig.Grey_color)
mMesiboMessage = mesiboMessage
mMesiborecyclerView = OutView
mMessageTypeIncoming = true
mMid = mesiboMessage.mid
mMidToDeleteList.add(mMid)
 //mesiboRecycleViewHolderArrayList.add(IncomingView);


                    (Objects.requireNonNull<FragmentActivity>(getActivity()) as MessagingActivityNew).Mesibo_onShowInContextUserInterface()
 //                        }else{
 //                            IncomingView.mRootLayout.setBackgroundColor(AppConfig.Transparent);
 //                            ((MessagingActivityNew) Objects.requireNonNull(getActivity())).Mesibo_onHideInContextUserInterface();
 //                        }


                    return true
}
})


}

}


public override fun Mesibo_onViewRecycled(mesiboRecycleViewHolder:MesiboRecycleViewHolder) {

}


public override fun Mesibo_oUpdateViewHolder(mesiboRecycleViewHolder:MesiboRecycleViewHolder, mesiboMessage:Mesibo.MesiboMessage) {

}


public override fun Mesibo_onFile(messageParams:Mesibo.MessageParams, fileInfo:Mesibo.FileInfo) {
super.Mesibo_onFile(messageParams, fileInfo)


}


public override fun Mesibo_onFileTransferProgress(file:Mesibo.FileInfo):Boolean {
if (100 == file.getProgress())
{

if (null != mProgressBar)
{
mProgressBar!!.setVisibility(View.GONE)
}
}
else
{

if (null != mProgressBar)
{
mProgressBar!!.setProgress(file.getProgress())
}
}
Log.d("*************Progress", (file.getProgress()).toString())

return true
}


public override fun Mesibo_onGetEnabledActionItems():Int {


return mHideRepy
}

 fun setPicture(view:ImageView) {


}

 fun onActionItemClicked(ItemId:Int) {

if (ItemId == AppConfig.MESSAGECONTEXTACTION_COPY)
{

val var17 = this.myActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
val var18 = ClipData.newPlainText("Copy", mMesiboMessage.message)
var17.setPrimaryClip(var18)

Toast.makeText(mActivity, "Copied", Toast.LENGTH_SHORT).show()

}
else if (ItemId == AppConfig.MESSAGECONTEXTACTION_REPLY)
{
replyEnabled = true

Toast.makeText(mActivity, "Reply", Toast.LENGTH_SHORT).show()
}
else if (ItemId == AppConfig.MESSAGECONTEXTACTION_DELETE)
{

val dialog = Dialog(getActivity()!!)
dialog.setCancelable(true)


dialog.setContentView(R.layout.delete_message_row_item)

val deleteForME = dialog.findViewById<TextView>(R.id.deleteForMe)
val cancel = dialog.findViewById<TextView>(R.id.cancelDelete)
val deleteForEveryOne = dialog.findViewById<TextView>(R.id.deleteForEveryone)

if (mMessageTypeIncoming)
{
deleteForEveryOne.setVisibility(View.GONE)
}


deleteForME.setOnClickListener(object:View.OnClickListener {
public override fun onClick(view:View) {

dialog.dismiss()
deleteMessage(Mesibo.MESIBO_DELETE_DEFAULT)


}
})

cancel.setOnClickListener(object:View.OnClickListener {
public override fun onClick(view:View) {
 //Do something


                    dialog.dismiss()


}
})


deleteForEveryOne.setOnClickListener(object:View.OnClickListener {
public override fun onClick(view:View) {
dialog.dismiss()
deleteMessage(Mesibo.MESIBO_DELETE_RECALL)


}
})

dialog.show()


}
else if (ItemId == AppConfig.MESSAGECONTEXTACTION_FORWARD)
{

 //MesiboUI.launchForwardActivity(Objects.requireNonNull(getActivity()), mMesiboMessage.message, true);


        }
else if (ItemId == AppConfig.MESSAGECONTEXTACTION_SHARE)
{
try
{


val i = Intent(Intent.ACTION_SEND)
i.setType("text/plain")
 //i.putExtra(Intent.EXTRA_SUBJECT, "WyngIt");
                val sAux = mMesiboMessage.message
 //sAux = sAux + "https://play.google.com/store/apps/details?id=the.package.id \n\n";
                i.putExtra(Intent.EXTRA_TEXT, sAux)
getActivity()!!.startActivity(Intent.createChooser(i, "Select One"))
}
catch (e:Exception) {
 //e.toString();
            }

}


}


 fun deleteMessage(Type:Int) {

if (null != mMesiborecyclerView)
{
try
{


for (i in mesiboRecycleViewHolderArrayList.indices)
{

mesiboRecycleViewHolderArrayList.get(i).delete(Type)
mMesiborecyclerView!!.delete(1)
}

for (j in mMidToDeleteList.indices)
{
Mesibo.deleteMessage(mMidToDeleteList.get(j), Type)
}

mesiboRecycleViewHolderArrayList.clear()
mRecyclerPosArrayList.clear()
mMidToDeleteList.clear()

mMesiborecyclerView!!.refresh()
}
catch (e:IndexOutOfBoundsException) {
e.printStackTrace()

}

}


}

public override fun Mesibo_onMessage(messageParams:Mesibo.MessageParams, bytes:ByteArray):Boolean {


return super.Mesibo_onMessage(messageParams, bytes)
}


public override fun Mesibo_onMessageStatus(messageParams:Mesibo.MessageParams?) {
super.Mesibo_onMessageStatus(messageParams)
}


private fun getTIME(time:Long):String {
val cal = Calendar.getInstance(Locale.ENGLISH)
cal.setTimeInMillis(time)
return DateFormat.format("hh:mm a", cal).toString()
}





 fun downloadFile(url:String?, UserImage:ImageView, filePath:String):Boolean {
val http = Mesibo.Http()
http.url = url
http.downloadFile = filePath
http.resume = true
http.maxRetries = 10
 //http.other = myObject;
        //file.setFileTransferContext(http);
        http.onMainThread = true
http.listener = object:Mesibo.HttpListener {
public override fun Mesibo_onHttpProgress(http:Mesibo.Http, state:Int, percent:Int):Boolean {
if (100 == percent && Mesibo.Http.STATE_DOWNLOAD == state)
{
 // download complete
                    val imgFile = File(filePath)
if (imgFile.exists())
{
val myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath())
if (saveBitmpToFilePath(myBitmap, filePath))
{}
UserImage.setImageBitmap(myBitmap)
}

return true
}
return true // return false to cancel
}
}

if (http.execute())
{}

return true
}

companion object {


 val TYPE_SEND_CONATCT = 113
 val TYPE_RECEIVE_CONTACT = 114
 var mDefaultLayout = true


 fun saveBitmpToFilePath(bmp:Bitmap?, filePath:String):Boolean {
val file = File(filePath)
var fOut:FileOutputStream? = null
try
{
fOut = FileOutputStream(file)
}
catch (e:FileNotFoundException) {
e.printStackTrace()
return false
}

if (null != bmp)
{
bmp!!.compress(Bitmap.CompressFormat.JPEG, 40, fOut)

try
{
fOut!!.flush()
}
catch (e:IOException) {
e.printStackTrace()
}

try
{
fOut!!.close()
}
catch (e:IOException) {
e.printStackTrace()
}

}

return true
}
}

}