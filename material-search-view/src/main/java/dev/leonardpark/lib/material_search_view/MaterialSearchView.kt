package dev.leonardpark.lib.material_search_view

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.Drawable
import android.os.Build
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View.OnClickListener
import android.view.ViewAnimationUtils
import android.widget.TextView
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatImageView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import dev.leonardpark.lib.material_search_view.databinding.SearchLayoutBinding
import kotlin.math.hypot

class MaterialSearchView : CardView {
  companion object {
    private const val ANIMATION_DURATION = 250
  }

  private var animateSearchView = false
  private var searchMenuPosition = 0
  private var searchHint: String? = null
  private var searchTextColor = 0
  private var searchIconColor: Int = 0
  private var mOldQueryText: CharSequence? = null
  private var mUserQuery: CharSequence? = null
  private var hasAdapter = false
  private var hideSearch = false

  private lateinit var binding: SearchLayoutBinding
  private var listenerQuery: OnQueryTextListener? = null
  private var visibilityListener: OnVisibilityListener? = null
  private var micClickListener: OnMicClickListener? = null

  constructor(context: Context) : super(context) {
    init(context, null)
  }

  constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
    init(context, attrs)
  }

  constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
    context,
    attrs,
    defStyleAttr
  ) {
    init(context, attrs)
  }

  private fun init(context: Context, attrs: AttributeSet?) {
    val a = context.theme.obtainStyledAttributes(attrs, R.styleable.MaterialSearchView, 0, 0)

    val inflater = LayoutInflater.from(context)
    val isDark = Utils.isUsingNightModeResources(context)
    binding = SearchLayoutBinding.inflate(inflater, this, true)
    animateSearchView = a.getBoolean(R.styleable.MaterialSearchView_search_animate, true)
    searchMenuPosition = a.getInteger(R.styleable.MaterialSearchView_search_menu_position, 0)
    searchHint = a.getString(R.styleable.MaterialSearchView_search_hint)
    searchTextColor = a.getColor(
      R.styleable.MaterialSearchView_search_text_color,
      ContextCompat.getColor(context, if (isDark) android.R.color.white else android.R.color.black)
    )
    searchIconColor = a.getColor(
      R.styleable.MaterialSearchView_search_icon_color,
      ContextCompat.getColor(context, if (isDark) android.R.color.white else android.R.color.black)
    )

    binding.imgBack.setOnClickListener(mOnClickListener)
    binding.imgClear.setOnClickListener(mOnClickListener)
    binding.imgMic.setOnClickListener(mOnClickListener)
    binding.editText.addTextChangedListener(mTextWatcher)
    binding.editText.setOnEditorActionListener(mOnEditorActionListener)

    val imeOptions = a.getInt(R.styleable.MaterialSearchView_search_ime_options, -1)
    if (imeOptions != -1) {
      setImeOptions(imeOptions)
    }

    val inputType = a.getInt(R.styleable.MaterialSearchView_search_input_type, -1)
    if (inputType != -1) {
      setInputType(inputType)
    }

    val focusable: Boolean = a.getBoolean(R.styleable.MaterialSearchView_search_focusable, true)
    binding.editText.isFocusable = focusable

    a.recycle()

    binding.editText.hint = getSearchHint()
    binding.editText.setTextColor(getTextColor())
    setDrawableTint(binding.imgBack.drawable, searchIconColor)
    setDrawableTint(binding.imgClear.drawable, searchIconColor)
    checkForAdapter()
  }

  private val mOnClickListener = OnClickListener { v ->
    when (v) {
      binding.imgClear -> setSearchText(null)
      binding.imgBack -> hideSearch()
      binding.imgMic -> {
        Log.d("testmo", "mo?")
        micClickListener?.onMicClick()
      }
    }
  }

  /**
   * Callback to watch the text field for empty/non-empty
   */
  private var mTextWatcher: TextWatcher? = object : TextWatcher {
    override fun beforeTextChanged(s: CharSequence, start: Int, before: Int, after: Int) {}
    override fun onTextChanged(s: CharSequence, start: Int, before: Int, after: Int) {
      submitText(s)
    }

    override fun afterTextChanged(s: Editable) {}
  }

  private val mOnEditorActionListener = TextView.OnEditorActionListener { _, _, _ ->
    onSubmitQuery()
    true
  }

  private fun submitText(s: CharSequence) {
    mUserQuery = binding.editText.text
    updateClearButton()
    if (listenerQuery != null && !TextUtils.equals(s, mOldQueryText)) {
      listenerQuery!!.onQueryTextChange(s.toString())
    }
    mOldQueryText = s.toString()
  }

  fun onSubmitQuery() {
    if (listenerQuery != null) {
      binding.linearItemsHolder.visibility = GONE
      listenerQuery!!.onQueryTextSubmit(binding.editText.text.toString())
    }
  }

  private fun updateClearButton() {
    val hasText = !TextUtils.isEmpty(binding.editText.text)
    binding.imgClear.visibility = if (hasText) VISIBLE else GONE
    binding.imgMic.visibility = if (micClickListener != null && hasText) {
      GONE
    } else {
      VISIBLE
    }
  }

  fun setQuery(query: CharSequence?, submit: Boolean) {
    binding.editText.setText(query)
    if (query != null) {
      mUserQuery = query
    }
    // If the query is not empty and submit is requested, submit the query
    if (submit && !TextUtils.isEmpty(query)) {
      onSubmitQuery()
    }
  }

  /**
   * Sets the IME options on the query text field.
   */
  fun setImeOptions(imeOptions: Int) {
    binding.editText.imeOptions = imeOptions
  }

  /**
   * Returns the IME options set on the query text field.
   */
  fun getImeOptions(): Int = binding.editText.imeOptions

  /**
   * Sets the input type on the query text field.
   */
  fun setInputType(inputType: Int) {
    binding.editText.inputType = inputType
  }

  /**
   * Returns the input type set on the query text field.
   */
  fun getInputType(): Int = binding.editText.inputType

  fun isVisible(): Boolean = visibility == VISIBLE

  fun setSearchText(queryText: String?) {
    binding.editText.setText(queryText)
  }

  fun setSearchRecyclerAdapter(adapter: Adapter<*>) {
    binding.recycler.adapter = adapter
    checkForAdapter()
  }

  fun hideRecycler() {
    hideSearch = true
    binding.linearItemsHolder.visibility = GONE
  }

  fun showRecycler() {
    hideSearch = false
    binding.linearItemsHolder.visibility = VISIBLE
  }

  fun showSearch() {
    hideSearch = false
    checkForAdapter()
    visibility = VISIBLE
    if (animateSearchView) if (Build.VERSION.SDK_INT >= 21) {
      val animatorShow = ViewAnimationUtils.createCircularReveal(
        this,  // view
        getCenterX(),  // center x
        convertDpToPixel(23f).toInt(), 0f,  // start radius
        hypot(width.toDouble(), height.toDouble()).toFloat() // end radius
      )
      animatorShow.addListener(object : AnimatorListenerAdapter() {
        override fun onAnimationEnd(animation: Animator) {
          super.onAnimationEnd(animation)
          visibilityListener?.onOpen()
          if (hasAdapter) {
            binding.linearItemsHolder.visibility = VISIBLE
          }
        }
      })
      animatorShow.start()
    } else {
      if (hasAdapter) {
        binding.linearItemsHolder.visibility = VISIBLE
      }
    }
  }

  fun hideSearch() {
    checkForAdapter()
    if (hasAdapter) {
      binding.linearItemsHolder.visibility = GONE
    }
    if (animateSearchView) {
      if (Build.VERSION.SDK_INT >= 21) {
        val animatorHide = ViewAnimationUtils.createCircularReveal(
          this,  // View
          getCenterX(),  // center x
          convertDpToPixel(23f).toInt(),  // center y
          hypot(width.toDouble(), height.toDouble()).toFloat(), 0f
        )
        animatorHide.startDelay = (if (hasAdapter) ANIMATION_DURATION else 0).toLong()
        animatorHide.addListener(object : AnimatorListenerAdapter() {
          override fun onAnimationEnd(animation: Animator) {
            super.onAnimationEnd(animation)
            visibilityListener?.onClose()
            visibility = GONE
          }
        })
        animatorHide.start()
      } else {
        visibility = GONE
      }
    }
  }

  fun setMenuPosition(menuPosition: Int) {
    searchMenuPosition = menuPosition
    invalidate()
    requestFocus()
  }

  // search searchHint
  fun getSearchHint(): String? = if (TextUtils.isEmpty(searchHint)) "Search" else searchHint

  fun setSearchHint(searchHint: String?) {
    this.searchHint = searchHint
    invalidate()
    requestFocus()
  }

  // text color
  fun setTextColor(textColor: Int) {
    searchTextColor = textColor
    invalidate()
    requestFocus()
  }

  fun setSearchIconColor(searchIconColor: Int) {
    this.searchIconColor = searchIconColor
    invalidate()
    requestFocus()
  }

  fun getTextColor(): Int = searchTextColor

  /**
   * Get views
   */
  fun getEditText(): AppCompatEditText = binding.editText

  fun getImageBack(): AppCompatImageView = binding.imgBack

  fun getImageClear(): AppCompatImageView = binding.imgClear

  fun getImageMic(): AppCompatImageView = binding.imgMic

  fun getRecyclerView(): RecyclerView = binding.recycler

  /**
   * Interface
   */
  fun addQueryTextListener(listenerQuery: OnQueryTextListener) {
    this.listenerQuery = listenerQuery
  }

  interface OnQueryTextListener {
    fun onQueryTextSubmit(query: String?): Boolean
    fun onQueryTextChange(newText: String?): Boolean
  }

  fun addOnVisibilityListener(visibilityListener: OnVisibilityListener) {
    this.visibilityListener = visibilityListener
  }

  interface OnVisibilityListener {
    fun onOpen(): Boolean
    fun onClose(): Boolean
  }

  fun addOnMicClickListener(micClickListener: OnMicClickListener) {
    this.micClickListener = micClickListener
  }

  interface OnMicClickListener {
    fun onMicClick()
  }

  /**
   * Helpers
   */
  private fun setDrawableTint(resDrawable: Drawable, resColor: Int) {
    resDrawable.colorFilter = PorterDuffColorFilter(resColor, PorterDuff.Mode.SRC_ATOP)
    resDrawable.mutate()
  }

  private fun convertDpToPixel(dp: Float): Float {
    return dp * (context.resources.displayMetrics.densityDpi / 160f)
  }

  private fun checkForAdapter() {
    hasAdapter =
      !hideSearch && binding.recycler.adapter != null && binding.recycler.adapter!!.itemCount > 0
  }

  /*
    TODO not correct but close
    Need to do correct measure
     */
  private fun getCenterX(): Int {
    val icons = (width - convertDpToPixel((21 * (1 + searchMenuPosition)).toFloat())).toInt()
    val padding = convertDpToPixel((searchMenuPosition * 21).toFloat()).toInt()
    return icons - padding
  }
}