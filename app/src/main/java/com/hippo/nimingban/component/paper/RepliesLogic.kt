/*
 * Copyright 2017 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.nimingban.component.paper

import android.text.style.ClickableSpan
import android.text.style.URLSpan
import com.hippo.nimingban.NMB_CLIENT
import com.hippo.nimingban.client.data.Reply
import com.hippo.nimingban.client.data.Thread
import com.hippo.nimingban.component.NmbLogic
import com.hippo.nimingban.component.NmbScene
import com.hippo.nimingban.component.openUrl
import com.hippo.nimingban.component.scene.galleryScene
import com.hippo.nimingban.util.INVALID_INDEX
import com.hippo.nimingban.widget.content.ContentData
import com.hippo.nimingban.widget.content.ContentDataAdapter
import com.hippo.nimingban.widget.content.ContentLayout
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

/*
 * Created by Hippo on 6/20/2017.
 */

abstract class RepliesLogic(
    private val id: String?,
    private var thread: Thread?,
    private var forum: String?,
    private val scene: NmbScene
) : NmbLogic() {

  var repliesUi: RepliesUi? = null

  private val data = RepliesData()

  init {
    data.restore()
  }

  /** Set data for adapter **/
  fun initializeAdapter(adapter: ContentDataAdapter<Reply, *>) {
    adapter.data = data
  }

  /** Unset data for adapter **/
  fun terminateAdapter(adapter: ContentDataAdapter<Reply, *>) {
    adapter.data = null
  }

  /** Set presenter for ContentLayout **/
  fun initializeContentLayout(contentLayout: ContentLayout) {
    data.ui = contentLayout
  }

  /** Unset presenter for ContentLayout **/
  fun terminateContentLayout(contentLayout: ContentLayout) {
    data.ui = null
  }

  /** Called when user click a thumb **/
  fun onClickThumb(reply: Reply) {
    scene.stage?.pushScene(reply.galleryScene())
  }

  /** Called when user click a span **/
  fun onClickSpan(span: ClickableSpan) {
    when(span) {
      is URLSpan -> openUrl(span.url)
    }
  }

  /** Called when user click a reply **/
  fun onClickReply(reply: Reply) {
    // TODO
  }

  fun getMinPage() = data.minPage

  fun getMaxPage() = data.maxPage

  fun getCurrentPage() = data.getPageForPosition(repliesUi?.findFirstVisibleItemPosition() ?: INVALID_INDEX)

  fun isLoaded() = data.isLoaded()

  fun onGoTo(page: Int) {
    data.switchTo(page)
  }

  /**
   * Called when the thread updated.
   */
  abstract fun onUpdateThread(thread: Thread)

  /**
   * Called when the forum updated.
   */
  abstract fun onUpdateForum(forum: String)


  private inner class RepliesData : ContentData<Reply>() {

    private val threadId get() = id ?: thread?.id

    override fun onRequireData(id: Int, page: Int) {
      val threadId = this.threadId
      if (threadId != null) {
        NMB_CLIENT.replies(threadId, page)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ (thread, replies, forum, pages) ->
              // Update thread
              if (this@RepliesLogic.thread == null) {
                this@RepliesLogic.thread = thread
                onUpdateThread(thread)
              }
              // Update forum
              if (!forum.isNullOrBlank() && this@RepliesLogic.forum != forum) {
                this@RepliesLogic.forum = forum
                onUpdateForum(forum)
              }
              setData(id, replies, pages)
            }, {
              setError(id, it)
            })
            .register()
      } else {
        // TODO i18n
        schedule { setError(id, Exception("No thread id")) }
      }
    }

    override fun onRestoreData(id: Int) {
      val thread = this@RepliesLogic.thread
      if (thread != null) {
        setData(id, listOf(thread.toReply()), 1)
      } else {
        setError(id, ContentData.FAILED_TO_RESTORE_EXCEPTION)
      }
    }

    override fun onBackupData(data: List<Reply>) {}
  }
}
