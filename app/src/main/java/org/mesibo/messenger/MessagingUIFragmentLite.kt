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

import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

import com.mesibo.api.Mesibo
import com.mesibo.messaging.MesiboMessagingFragment
import com.mesibo.messaging.MesiboRecycleViewHolder

import java.util.Calendar
import java.util.Locale


class MessagingUIFragmentLite : MesiboMessagingFragment(), MesiboRecycleViewHolder.Listener, Mesibo.FileTransferListener {
    var mTYPE = 0


    override fun Mesibo_onGetItemViewType(messageParams: Mesibo.MessageParams, s: String): Int {


        if (messageParams.isIncoming) {
            return MesiboRecycleViewHolder.TYPE_INCOMING
        }

        if (messageParams.status == MesiboRecycleViewHolder.TYPE_HEADER) {
            return MesiboRecycleViewHolder.TYPE_HEADER
        }

        if (messageParams.isSavedMessage) {
            return MesiboRecycleViewHolder.TYPE_CUSTOM
        }

        return if (messageParams.isMissedCall) {
            MesiboRecycleViewHolder.TYPE_MISSEDCALL
        } else MesiboRecycleViewHolder.TYPE_OUTGOING


        // return 0;
    }


    inner class HeaderViewHolder(internal var mViewHeader: View) : MesiboRecycleViewHolder(mViewHeader)

    inner class SendContactViewHolder(internal var mViewSendContact: View) : MesiboRecycleViewHolder(mViewSendContact) {

        internal var mContactName: TextView
        internal var mMessageContact: TextView
        internal var mContactImage: ImageView

        init {
            mContactName = mViewSendContact.findViewById(R.id.contactName)
            mMessageContact = mViewSendContact.findViewById(R.id.messageContact)
            mContactImage = mViewSendContact.findViewById(R.id.contactImage)


        }
    }


    inner class ReceiveContactViewHolder(internal var mViewReceiveContact: View) : MesiboRecycleViewHolder(mViewReceiveContact) {

        internal var mContactName: TextView
        internal var mMessageContact: TextView
        internal var mAddToContact: TextView
        internal var mContactImage: ImageView

        init {
            mContactName = mViewReceiveContact.findViewById(R.id.contactName)
            mMessageContact = mViewReceiveContact.findViewById(R.id.messageTV)
            mAddToContact = mViewReceiveContact.findViewById(R.id.addToContactTV)
            mContactImage = mViewReceiveContact.findViewById(R.id.contactImage)


        }
    }


    inner class MissedCallViewHolder(internal var mMissedViewHeader: View) : MesiboRecycleViewHolder(mMissedViewHeader) {

        internal var missedTS: TextView
        internal var callTypeText: TextView
        internal var customMessageTV: TextView
        internal var missedCallImage: ImageView
        internal var mMissedCallLayout: LinearLayout
        internal var mCustomMessageLayout: LinearLayout

        init {


            mMissedCallLayout = mMissedViewHeader.findViewById(R.id.missedCallLayout)
            mCustomMessageLayout = mMissedViewHeader.findViewById(R.id.customeMessageLayout)
            customMessageTV = mMissedViewHeader.findViewById(R.id.customeMessage)
            missedTS = mMissedViewHeader.findViewById(R.id.missedcallTS)
            callTypeText = mMissedViewHeader.findViewById(R.id.callTypeMsg)
            missedCallImage = mMissedViewHeader.findViewById(R.id.missedCallImage)


        }
    }


    override fun Mesibo_onCreateViewHolder(viewGroup: ViewGroup, type: Int): MesiboRecycleViewHolder? {


        if (TYPE_SEND_CONATCT == type) {


            val v = LayoutInflater.from(viewGroup.context).inflate(R.layout.send_contact_row_item, viewGroup, false)
            return SendContactViewHolder(v)

        } else if (TYPE_RECEIVE_CONTACT == type) {

            val v = LayoutInflater.from(viewGroup.context).inflate(R.layout.receive_contact_row_item, viewGroup, false)
            return ReceiveContactViewHolder(v)

        } else if (MesiboRecycleViewHolder.TYPE_HEADER == type) {

            val v = LayoutInflater.from(viewGroup.context).inflate(R.layout.chat_header_view, viewGroup, false)
            return HeaderViewHolder(v)

        } else if (MesiboRecycleViewHolder.TYPE_MISSEDCALL == type) {

            val v = LayoutInflater.from(viewGroup.context).inflate(R.layout.chat_missedcall_view, viewGroup, false)
            return MissedCallViewHolder(v)

        }


        return null
    }


    override fun Mesibo_onBindViewHolder(mesiboRecycleViewHolder: MesiboRecycleViewHolder, type: Int, b: Boolean, messageParams: Mesibo.MessageParams, mesiboMessage: Mesibo.MesiboMessage) {


        if (type == MesiboRecycleViewHolder.TYPE_HEADER) {

            mTYPE = MesiboRecycleViewHolder.TYPE_HEADER
            val HeaderView = mesiboRecycleViewHolder as HeaderViewHolder

        } else if (type == MesiboRecycleViewHolder.TYPE_MISSEDCALL) {
            mTYPE = type
            val missedCallViewHolder = mesiboRecycleViewHolder as MissedCallViewHolder
            missedCallViewHolder.mCustomMessageLayout.visibility = View.GONE
            missedCallViewHolder.mMissedCallLayout.visibility = View.VISIBLE
            missedCallViewHolder.missedTS.text = getTIME(mesiboMessage.ts)

            if (messageParams.isVoiceCall) {
                missedCallViewHolder.callTypeText.text = "Missed audio call at"
                val mAudioMissedImage = R.drawable.baseline_call_missed_black_24
                missedCallViewHolder.missedCallImage.setImageResource(mAudioMissedImage)
            } else {
                missedCallViewHolder.callTypeText.text = "Missed video call at"
                val mVideoMissedImage = R.drawable.baseline_missed_video_call_black_24
                missedCallViewHolder.missedCallImage.setImageResource(mVideoMissedImage)
            }

        } else if (type == MesiboRecycleViewHolder.TYPE_CUSTOM) {

            val customMessage = mesiboRecycleViewHolder as MissedCallViewHolder
            customMessage.mCustomMessageLayout.visibility = View.VISIBLE
            customMessage.mMissedCallLayout.visibility = View.GONE
            customMessage.customMessageTV.text = mesiboMessage.message

        }


    }


    override fun Mesibo_onViewRecycled(mesiboRecycleViewHolder: MesiboRecycleViewHolder) {

    }

    override fun Mesibo_oUpdateViewHolder(mesiboRecycleViewHolder: MesiboRecycleViewHolder, mesiboMessage: Mesibo.MesiboMessage) {

    }

    private fun getTIME(time: Long): String {
        val cal = Calendar.getInstance(Locale.ENGLISH)
        cal.timeInMillis = time
        return DateFormat.format("hh:mm a", cal).toString()
    }

    companion object {


        val TYPE_SEND_CONATCT = 113
        val TYPE_RECEIVE_CONTACT = 114
    }


}