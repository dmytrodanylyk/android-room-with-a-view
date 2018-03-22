package com.example.android.roomwordssample.ui;

/*
 * Copyright (C) 2017 Google Inc.
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

import android.annotation.SuppressLint
import android.app.Activity
import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.os.Looper
import android.support.design.widget.FloatingActionButton
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.example.android.roomwordssample.R
import com.example.android.roomwordssample.model.Word
import com.example.android.roomwordssample.viewmodel.WordViewModel
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.channels.LinkedListChannel
import kotlinx.coroutines.experimental.channels.SubscriptionReceiveChannel
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.launch


class MainActivity : AppCompatActivity() {

    private lateinit var wordViewModel: WordViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerview)
        val adapter = WordListAdapter(this)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Get a new or existing ViewModel from the ViewModelProvider.
        wordViewModel = ViewModelProviders.of(this).get(WordViewModel::class.java)

        // Add an observer on the LiveData returned by getAlphabetizedWords.
        // The onChanged() method fires when the observed data changes and the activity is
        // in the foreground.
//        wordViewModel.allWords.observe(this, Observer { words ->
//            // Update the cached copy of the words in the adapter.
//            adapter.setWords(words)
//        })

        val channel = wordViewModel.allWords.channel(this)
        launch(UI) {
            channel.consumeEach { words: List<Word>? ->
                logThread()
                adapter.setWords(words)
//                wordViewModel.allWords.removeObserver(channel)
            }
        }

        val fab = findViewById<FloatingActionButton>(R.id.fab)
        fab.setOnClickListener {
            val intent = Intent(this@MainActivity, NewWordActivity::class.java)
            startActivityForResult(intent, NEW_WORD_ACTIVITY_REQUEST_CODE)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_settings -> true
        else -> super.onOptionsItemSelected(item)
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == NEW_WORD_ACTIVITY_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val word = Word(data.getStringExtra(NewWordActivity.EXTRA_REPLY))
            wordViewModel.insert(word)
        } else {
            Toast.makeText(applicationContext, R.string.empty_not_saved, Toast.LENGTH_LONG).show()
        }
    }

    companion object {
        val NEW_WORD_ACTIVITY_REQUEST_CODE = 1
    }
}

fun <T> LiveData<T>.channel(lifecycleOwner: LifecycleOwner): LiveDataChannel<T> {
    val channel = LiveDataChannel(this)
    observe(lifecycleOwner, channel)
    return channel
}

fun <T> LiveData<T>.channel(): LiveDataChannel<T> {
    val channel = LiveDataChannel(this)
    observeForever(channel)
    return channel
}

// TODO how to close channel?
class LiveDataChannel<T>(private val liveData: LiveData<T>) : LinkedListChannel<T?>(), SubscriptionReceiveChannel<T?>, Observer<T?> {

    override fun onChanged(t: T?) {
        offer(t)
    }

    override fun afterClose(cause: Throwable?) {
        liveData.removeObserver(this)
    }

}

@SuppressLint("NewApi")
fun logThread() {
    Log.d("TEST", "Is main thread: " + Looper.getMainLooper().isCurrentThread)
}
