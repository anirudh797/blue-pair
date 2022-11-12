/*
 * MIT License
 * <p>
 * Copyright (c) 2017 Donato Rimenti
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package co.aurasphere.bluetooth.view

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.View
import android.widget.ProgressBar

/**
 * Custom RecyclerView with support for showing another view when there's no data and another
 * one for loading.
 *
 * @author Donato Rimenti
 */
class RecyclerViewProgressEmptySupport : RecyclerView {
    /**
     * The view to show if the list is empty.
     */
    private var emptyView: View? = null

    /**
     * Observer for list data. Sets the empty view if the list is empty.
     */
    private val emptyObserver: AdapterDataObserver = object : AdapterDataObserver() {
        override fun onChanged() {
            val adapter = adapter
            if (adapter != null && emptyView != null) {
                if (adapter.itemCount == 0) {
                    emptyView!!.visibility = VISIBLE
                    this@RecyclerViewProgressEmptySupport.visibility = GONE
                } else {
                    emptyView!!.visibility = GONE
                    this@RecyclerViewProgressEmptySupport.visibility = VISIBLE
                }
            }
        }
    }

    /**
     * View shown during loading.
     */
    private var progressView: ProgressBar? = null

    /**
     * @see RecyclerView.RecyclerView
     */
    constructor(context: Context?) : super(context!!) {}

    /**
     * @see RecyclerView.RecyclerView
     */
    constructor(context: Context?, attrs: AttributeSet?) : super(
        context!!, attrs
    ) {
    }

    /**
     * @see RecyclerView.RecyclerView
     */
    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(
        context!!, attrs, defStyle
    ) {
    }

    /**
     * {@inheritDoc}
     */
    override fun setAdapter(adapter: Adapter<*>?) {
        super.setAdapter(adapter)
        adapter?.registerAdapterDataObserver(emptyObserver)
        emptyObserver.onChanged()
    }

    /**
     * Sets the empty view.
     *
     * @param emptyView the [.emptyView]
     */
    fun setEmptyView(emptyView: View?) {
        this.emptyView = emptyView
    }

    /**
     * Sets the progress view.
     *
     * @param progressView the [.progressView].
     */
    fun setProgressView(progressView: ProgressBar?) {
        this.progressView = progressView
    }

    /**
     * Shows the progress view.
     */
    fun startLoading() {
        // Hides the empty view.
        if (emptyView != null) {
            emptyView!!.visibility = GONE
        }
        // Shows the progress bar.
        if (progressView != null) {
            progressView!!.visibility = VISIBLE
        }
    }

    /**
     * Hides the progress view.
     */
    fun endLoading() {
        // Hides the progress bar.
        if (progressView != null) {
            progressView!!.visibility = GONE
        }

        // Forces the view refresh.
        emptyObserver.onChanged()
    }
}