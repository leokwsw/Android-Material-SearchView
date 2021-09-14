package dev.leonardpark.app.materialSearchViewDemo

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import dev.leonardpark.app.materialSearchViewDemo.databinding.ActivityMainBinding
import dev.leonardpark.lib.material_search_view.MaterialSearchView
import dev.leonardpark.lib.material_search_view.VoiceSearchUtils

class MainActivity : AppCompatActivity() {

  private lateinit var binding: ActivityMainBinding

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    binding = ActivityMainBinding.inflate(layoutInflater)
    setContentView(binding.root)

    setSupportActionBar(binding.toolbar)

    binding.fab.setOnClickListener { view ->
      Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
        .setAction("Action", null).show()
    }

    initSearch()
  }

  private fun initSearch() {
    binding.search.apply {
      addQueryTextListener(object : MaterialSearchView.OnQueryTextListener {
        override fun onQueryTextSubmit(query: String?): Boolean {
          return true
        }

        override fun onQueryTextChange(newText: String?): Boolean {
          return true
        }
      })
      addOnMicClickListener(object : MaterialSearchView.OnMicClickListener {
        override fun onMicClick() {
          Log.d("testmo", "mo")
//          if (VoiceSearchUtils.isVoiceSearchAvailable(this@MainActivity)) {
            VoiceSearchUtils.setVoiceSearch(this@MainActivity, "Speak", "zh-HK")
//          } else {
//            Toast.makeText(this@MainActivity, "Voice Search is not Available", Toast.LENGTH_LONG).show()
//          }
        }
      })
    }
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    if(requestCode == VoiceSearchUtils.SPEECH_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
      val matches = data!!.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
      if(matches!= null){
        val text = matches.first()
        binding.search.setSearchText(text)
        Log.d("testmo", "text : $text")
      } else {
        Log.d("testmo", "text : ")
      }
    }
    super.onActivityResult(requestCode, resultCode, data)

  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    // Inflate the menu; this adds items to the action bar if it is present.
    menuInflater.inflate(R.menu.menu_main, menu)
    return true
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    return when (item.itemId) {
      R.id.action_search -> {
        binding.search.showSearch()
        true
      }
      else -> super.onOptionsItemSelected(item)
    }
  }


}