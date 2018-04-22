package com.example.nouman.echo.fragments

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import com.cleveroad.audiovisualization.AudioVisualization
import com.cleveroad.audiovisualization.DbmHandler
import com.cleveroad.audiovisualization.GLAudioVisualizationView
import com.example.nouman.echo.CurrentSongHelper
import com.example.nouman.echo.R
import com.example.nouman.echo.Songs
import java.util.*
import java.util.concurrent.TimeUnit


/**
 * A simple [Fragment] subclass.
 *
 */
class SongPlayingFragment : Fragment() {
    var myActivity: Activity? = null

    /*This is the media player variable. We would be using this to play/pause the music*/
    var mediaPlayer: MediaPlayer? = null

    /*The different variables defined will be used for their respective purposes*/
    /*Depending on the task they do we name the variables as such so that it gets easier to identify the task they perform*/
    var startTimeText: TextView? = null
    var endTimeText: TextView? = null
    var playPauseImageButton: ImageButton? = null
    var previousImageButton: ImageButton? = null
    var nextImageButton: ImageButton? = null
    var loopImageButton: ImageButton? = null
    var shuffleImageButton: ImageButton? = null
    var seekBar: SeekBar? = null
    var songArtistView: TextView? = null
    var songTitleView: TextView? = null
    var currentPosition: Int = 0
    var fetchSongs: ArrayList<Songs>? = null

    /*The current song helper is used to store the details of the current song being played*/
    var currentSongHelper: CurrentSongHelper? = null

    var audioVisualization: AudioVisualization? = null
    var glView: GLAudioVisualizationView? = null

    /*Variable used to update the song time*/
    var updateSongTime = object : Runnable{
        override fun run() {
            val getCurrent = mediaPlayer?.currentPosition
            startTimeText?.setText(String.format("%d:%d",
                    TimeUnit.MILLISECONDS.toMinutes(getCurrent?.toLong() as Long),
                    TimeUnit.MILLISECONDS.toSeconds(getCurrent?.toLong() as Long -
                    TimeUnit.MILLISECONDS.toSeconds(TimeUnit.MILLISECONDS.toMinutes(getCurrent?.toLong() as Long)))))

            seekBar?.setProgress(getCurrent?.toInt() as Int)
            Handler().postDelayed(this, 1000)
        }

    }

    /*Similar onCreateView() method of the fragment, which we used for the MainScreenFragment*/
    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view =  inflater!!.inflate(R.layout.fragment_song_playing, container, false)

        /*Linking views with their ids*/
        seekBar = view?.findViewById(R.id.seekBar)
        startTimeText = view?.findViewById(R.id.startTime)
        endTimeText = view?.findViewById(R.id.endTime)
        playPauseImageButton = view?.findViewById(R.id.playPauseButton)
        nextImageButton = view?.findViewById(R.id.nextButton)
        previousImageButton = view?.findViewById(R.id.prevoiusButton)
        loopImageButton = view?.findViewById(R.id.loopButton)
        shuffleImageButton = view?.findViewById(R.id.shuffleButton)
        songArtistView = view?.findViewById(R.id.songArtist)
        songTitleView = view?.findViewById(R.id.songTitle)
        glView = view?.findViewById(R.id.visualizer_view)


        return view
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        audioVisualization = glView as AudioVisualization
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        myActivity = context as Activity
    }

    override fun onAttach(activity: Activity?) {
        super.onAttach(activity)
        myActivity = activity
    }

    override fun onResume() {
        super.onResume()
        audioVisualization?.onResume()
    }

    override fun onPause() {
        super.onPause()
        audioVisualization?.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        audioVisualization?.release()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        currentSongHelper = CurrentSongHelper()
        currentSongHelper?.isPlaying = true
        currentSongHelper?.isLoop = false
        currentSongHelper?.isShuffle = false

        /*Now this is new. Let's see what is all this about*/
        /*These are the variables used for retrieving the Bundle items sent from the main screen
        * Now remember I told you to remember the names of these Bundle items, they will be used here*/
        var path: String? = null
        var _songTitle: String? = null
        var _songArtist: String? = null
        var songId: Long = 0

        /*See that we have used a try catch block here
        * The reason for doing so is that, it may happen that the bundle object does not have these in it and the app may crash
        * So in order to prevent the crash we use try-catch block. This block is known as the error-handling block*/
        try {

            /*path is retrieved using the same key (path) which was used to send it*/
            path = arguments.getString("path")

            /*song title retrieved with its key songTitle*/
            _songTitle = arguments.getString("songTitle")

            /*song artist with the key songArtist*/
            _songArtist = arguments.getString("songArtist")

            /*song id with the key SongId*/
            songId = arguments.getInt("songId").toLong()

            /*Here we fetch the received bundle data for current position and the list of all songs*/
                    currentPosition = arguments.getInt("songPosition")
            fetchSongs = arguments.getParcelableArrayList("songData")

            /*Now store the song details to the current song helper object so that they can be used later*/
            currentSongHelper?.songPath = path
            currentSongHelper?.songTitle = _songTitle
            currentSongHelper?.songArtist = _songArtist
            currentSongHelper?.songId = songId
            currentSongHelper?.currentPosition = currentPosition

        } catch (e: Exception) {
            e.printStackTrace()
        }

        /*here we initialise the media player object*/
        mediaPlayer = MediaPlayer()

        /*here we tell the media player object that we would be streaming the music*/
        mediaPlayer?.setAudioStreamType(AudioManager.STREAM_MUSIC)

        /*Here also we use the error-handling as the path we sent may return a null object*/
        try {

            /*The data source set the song to the media player object*/
            mediaPlayer?.setDataSource(myActivity, Uri.parse(path))

            /*Before plaing the music we prepare the media player for playback*/
            mediaPlayer?.prepare()

        } catch (e: Exception) {
            e.printStackTrace()
        }

        /*If all of the above goes well we start the music using the start() method*/
        mediaPlayer?.start()

        processInformation(mediaPlayer as MediaPlayer)

        if (currentSongHelper?.isPlaying as Boolean) {
            playPauseImageButton?.setBackgroundResource(R.drawable.pause_icon)
        } else {
            playPauseImageButton?.setBackgroundResource(R.drawable.play_icon)
        }
        mediaPlayer?.setOnCompletionListener {
            onSongComplete()
        }
        clickHandler()

        var visualizationHandler = DbmHandler.Factory.newVisualizerHandler(myActivity as Context, 0)
        audioVisualization?.linkTo(visualizationHandler)
    }
    /*A new click handler function is created to handle all the click functions in the song playing fragment*/
    fun clickHandler() {

        /*The implementation will be taught in the coming topics*/
        shuffleImageButton?.setOnClickListener({
            if(currentSongHelper?.isShuffle as Boolean){
                shuffleImageButton?.setBackgroundResource(R.drawable.shuffle_white_icon)
                currentSongHelper?.isShuffle = false
            } else{
                currentSongHelper?.isShuffle = true
                currentSongHelper?.isLoop = false
                shuffleImageButton?.setBackgroundResource(R.drawable.shuffle_icon)
                loopImageButton?.setBackgroundResource(R.drawable.loop_white_icon)
            }
        })
        nextImageButton?.setOnClickListener({
            currentSongHelper?.isPlaying = true
            if(currentSongHelper?.isShuffle as Boolean){
                playNext("PlayNextLikeNormalShuffle")
            } else {
                playNext("PlayNextNormal")
            }
        })
        previousImageButton?.setOnClickListener({
            /*We set the player to be playing by setting isPlaying to be true*/
            currentSongHelper?.isPlaying = true

            /*First we check if the loop is on or not*/
            if (currentSongHelper?.isLoop as Boolean) {

                /*If the loop was on we turn it off*/
                loopImageButton?.setBackgroundResource(R.drawable.loop_white_icon)
            }

            /*After all of the above is done we then play the previous song using the playPrevious() function*/
            playPrevious()
        })
        loopImageButton?.setOnClickListener({
            /*if loop was enabled, we turn it off and vice versa*/
            if (currentSongHelper?.isLoop as Boolean) {

                /*Making the isLoop false*/
                currentSongHelper?.isLoop = false

                /*We change the color of the icon*/
                loopImageButton?.setBackgroundResource(R.drawable.loop_white_icon)
            } else {

                /*If loop was not enabled when tapped, we enable if and make the isLoop to true*/
                currentSongHelper?.isLoop = true

                /*Loop and shuffle won't work together so we put shuffle false irrespectve of the whether it was on or not*/
                currentSongHelper?.isShuffle = false

                /*Loop button color changed to mark it ON*/
                loopImageButton?.setBackgroundResource(R.drawable.loop_icon)

                /*Changing the shuffle button to white, no matter which color it was earlier*/
                shuffleImageButton?.setBackgroundResource(R.drawable.shuffle_white_icon)
            }
        })

        /*Here we handle the click event on the play/pause button*/
        playPauseImageButton?.setOnClickListener({

            /*if the song is already playing and then play/pause button is tapped
            * then we pause the media player and also change the button to play button*/
            if (mediaPlayer?.isPlaying as Boolean) {
                mediaPlayer?.pause()
                currentSongHelper?.isPlaying = false
                playPauseImageButton?.setBackgroundResource(R.drawable.play_icon)

                /*If the song was not playing the, we start the music player and
                * change the image to pause icon*/
            } else {
                mediaPlayer?.start()

                processInformation(mediaPlayer as MediaPlayer)

                currentSongHelper?.isPlaying = true
                playPauseImageButton?.setBackgroundResource(R.drawable.pause_icon)
            }
        })
    }

    /*The playNext() function is used to play the next song*/
    fun playNext(check: String) {

        /*Let this one sit for a while, We'll explain this after the next section where we will be teaching to add the next and previous functionality*/
        if (check.equals("PlayNextNormal", true)) {
            currentPosition = currentPosition + 1
        } else if (check.equals("PlayNextLikeNormalShuffle", true)) {
            var randomObject = Random()
            var randomPosition = randomObject.nextInt(fetchSongs?.size?.plus(1) as Int)
            currentPosition = randomPosition
        }
        if (currentPosition == fetchSongs?.size) {
            currentPosition = 0
        }
        currentSongHelper?.isLoop = false
        var nextSong = fetchSongs?.get(currentPosition)
        currentSongHelper?.songPath = nextSong?.songData
        currentSongHelper?.songTitle = nextSong?.songTitle
        currentSongHelper?.songArtist = nextSong?.artist
        currentSongHelper?.songId = nextSong?.songID as Long

        /*updating the text views for title and artist name*/
        updateTextViews(currentSongHelper?.songTitle as String, currentSongHelper?.songArtist as String)

        mediaPlayer?.reset()
        try {
            mediaPlayer?.setDataSource(myActivity, Uri.parse(currentSongHelper?.songPath))
            mediaPlayer?.prepare()
            mediaPlayer?.start()

            processInformation(mediaPlayer as MediaPlayer)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /*The function playPrevious() is used to play the previous song again*/
    fun playPrevious() {

        /*Decreasing the current position by 1 to get the position of the previous song*/
        currentPosition = currentPosition - 1

        /*If the current position becomes less than 1, we make it 0 as there is no index as -1*/
        if (currentPosition == -1) {
            currentPosition = 0
        }
        if (currentSongHelper?.isPlaying as Boolean) {
            playPauseImageButton?.setBackgroundResource(R.drawable.pause_icon)
        } else {
            playPauseImageButton?.setBackgroundResource(R.drawable.play_icon)
        }
        currentSongHelper?.isLoop = false

        /*Similar to the playNext() function defined above*/
        var nextSong = fetchSongs?.get(currentPosition)
        currentSongHelper?.songPath = nextSong?.songData
        currentSongHelper?.songTitle = nextSong?.songTitle
        currentSongHelper?.songArtist = nextSong?.artist
        currentSongHelper?.songId = nextSong?.songID as Long

        /*updating the text views for title and artist name*/
        updateTextViews(currentSongHelper?.songTitle as String, currentSongHelper?.songArtist as String)

        mediaPlayer?.reset()
        try {
            mediaPlayer?.setDataSource(myActivity, Uri.parse(currentSongHelper?.songPath))
            mediaPlayer?.prepare()
            mediaPlayer?.start()

            processInformation(mediaPlayer as MediaPlayer)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /*Function to handle the event where the song completes playing*/
    fun onSongComplete() {

        /*If shuffle was on then play a random next song*/
        if (currentSongHelper?.isShuffle as Boolean) {
            playNext("PlayNextLikeNormalShuffle")
            currentSongHelper?.isPlaying = true
        } else {

            /*If loop was ON, then play the same ong again*/
            if (currentSongHelper?.isLoop as Boolean) {
                currentSongHelper?.isPlaying = true
                var nextSong = fetchSongs?.get(currentPosition)
                currentSongHelper?.currentPosition = currentPosition
                currentSongHelper?.songPath = nextSong?.songData
                currentSongHelper?.songTitle = nextSong?.songTitle
                currentSongHelper?.songArtist = nextSong?.artist
                currentSongHelper?.songId = nextSong?.songID as Long

                /*updating the text views for title and artist name*/
                updateTextViews(currentSongHelper?.songTitle as String, currentSongHelper?.songArtist as String)

                mediaPlayer?.reset()
                try {
                    mediaPlayer?.setDataSource(myActivity, Uri.parse(currentSongHelper?.songPath))
                    mediaPlayer?.prepare()
                    mediaPlayer?.start()

                    processInformation(mediaPlayer as MediaPlayer)

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {

                /*If loop was OFF then normally play the next song*/
                playNext("PlayNextNormal")
                currentSongHelper?.isPlaying = true
            }
        }
    }

    /*Function to update the views of songs and their artist names*/
    fun updateTextViews(songTitle: String, songArtist: String) {
        songTitleView?.setText(songTitle)
        songArtistView?.setText(songArtist)
    }

    /*function used to update the time*/
    fun processInformation(mediaPlayer: MediaPlayer) {

        /*Obtaining the final time*/
        val finalTime = mediaPlayer.duration

        /*Obtaining the current position*/
        val startTime = mediaPlayer.currentPosition

        seekBar?.max = finalTime

        /*Here we format the time and set it to the start time text*/
        startTimeText?.setText(String.format("%d: %d",
                TimeUnit.MILLISECONDS.toMinutes(startTime.toLong()),
                TimeUnit.MILLISECONDS.toSeconds(startTime.toLong()) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(startTime.toLong())))
        )

        /*Similar to above is done for the end time text*/
        endTimeText?.setText(String.format("%d: %d",
                TimeUnit.MILLISECONDS.toMinutes(finalTime.toLong()),
                TimeUnit.MILLISECONDS.toSeconds(finalTime.toLong()) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(finalTime.toLong())))
        )

        /*Seekbar has been assigned this time so that it moves according to the time of song*/
        seekBar?.setProgress(startTime)

        /*Now this task is synced with the update song time obhect*/
        Handler().postDelayed(updateSongTime, 1000)
    }
}

