package com.example.nouman.echo.fragments

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import com.cleveroad.audiovisualization.AudioVisualization
import com.cleveroad.audiovisualization.DbmHandler
import com.cleveroad.audiovisualization.GLAudioVisualizationView
import com.example.nouman.echo.CurrentSongHelper
import com.example.nouman.echo.R
import com.example.nouman.echo.R.id.seekBar
import com.example.nouman.echo.Songs
import com.example.nouman.echo.databases.EchoDatabase
import com.example.nouman.echo.fragments.SongPlayingFragment.Staticated.onSongComplete
import com.example.nouman.echo.fragments.SongPlayingFragment.Staticated.playNext
import com.example.nouman.echo.fragments.SongPlayingFragment.Staticated.processInformation
import com.example.nouman.echo.fragments.SongPlayingFragment.Staticated.updateTextViews
import com.example.nouman.echo.fragments.SongPlayingFragment.Statified.audioVisualization
import com.example.nouman.echo.fragments.SongPlayingFragment.Statified.currentPosition
import com.example.nouman.echo.fragments.SongPlayingFragment.Statified.currentSongHelper
import com.example.nouman.echo.fragments.SongPlayingFragment.Statified.endTimeText
import com.example.nouman.echo.fragments.SongPlayingFragment.Statified.fab
import com.example.nouman.echo.fragments.SongPlayingFragment.Statified.favoriteContent
import com.example.nouman.echo.fragments.SongPlayingFragment.Statified.fetchSongs
import com.example.nouman.echo.fragments.SongPlayingFragment.Statified.glView
import com.example.nouman.echo.fragments.SongPlayingFragment.Statified.loopImageButton
import com.example.nouman.echo.fragments.SongPlayingFragment.Statified.mediaPlayer
import com.example.nouman.echo.fragments.SongPlayingFragment.Statified.myActivity
import com.example.nouman.echo.fragments.SongPlayingFragment.Statified.nextImageButton
import com.example.nouman.echo.fragments.SongPlayingFragment.Statified.playPauseImageButton
import com.example.nouman.echo.fragments.SongPlayingFragment.Statified.previousImageButton
import com.example.nouman.echo.fragments.SongPlayingFragment.Statified.shuffleImageButton
import com.example.nouman.echo.fragments.SongPlayingFragment.Statified.songArtistView
import com.example.nouman.echo.fragments.SongPlayingFragment.Statified.songTitleView
import com.example.nouman.echo.fragments.SongPlayingFragment.Statified.startTimeText
import com.example.nouman.echo.fragments.SongPlayingFragment.Statified.updateSongTime
import java.util.*
import java.util.concurrent.TimeUnit


/**
 * A simple [Fragment] subclass.
 *
 */
class SongPlayingFragment : Fragment() {
    /*Here you may wonder that why did we create two objects namely Statified and Staticated respectively
    * These objects are created as the variables and functions will be used from another class
    * Now, the question is why did we make two different objects and not one single object
    * This is because we created the Statified object which contains all the variables and
    * the Staticated object which contain all the functions*/
    object Statified {
        var myActivity: Activity? = null
        var mediaPlayer: MediaPlayer? = null
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
        var currentSongHelper: CurrentSongHelper? = null

        /*Declaring variable for handling the favorite button*/
        var fab: ImageButton? = null

        /*Variable for using DB functions*/
        var favoriteContent: EchoDatabase? = null
        var audioVisualization: AudioVisualization? = null
        var glView: GLAudioVisualizationView? = null

        var updateSongTime = object : Runnable {
            override fun run() {
                val getCurrent = mediaPlayer?.currentPosition
                startTimeText?.setText(String.format("%d:%d",
                        TimeUnit.MILLISECONDS.toMinutes(getCurrent?.toLong() as Long),
                        TimeUnit.MILLISECONDS.toSeconds(TimeUnit.MILLISECONDS.toMinutes(getCurrent?.toLong() as Long))))
                seekBar?.setProgress(getCurrent?.toInt() as Int)
                Handler().postDelayed(this, 1000)
            }
        }
    }

    /*Declaring the preferences for the shuffle and loop feature
    * the object is created as we will need them outside the scope of this class*/
    object Staticated {
        var MY_PREFS_SHUFFLE = "Shuffle feature"
        var MY_PREFS_LOOP = "Loop feature"

        /*Function to handle the event where the song completes playing*/
        fun onSongComplete() {

            /*If shuffle was on then play a random next song*/
            if (Statified.currentSongHelper?.isShuffle as Boolean) {
                playNext("PlayNextLikeNormalShuffle")
                Statified.currentSongHelper?.isPlaying = true
            } else {

                /*If loop was ON, then play the same ong again*/
                if (Statified.currentSongHelper?.isLoop as Boolean) {
                    Statified.currentSongHelper?.isPlaying = true
                    var nextSong = Statified.fetchSongs?.get(Statified.currentPosition)
                    Statified.currentSongHelper?.currentPosition = Statified.currentPosition
                    Statified.currentSongHelper?.songPath = nextSong?.songData
                    Statified.currentSongHelper?.songTitle = nextSong?.songTitle
                    Statified.currentSongHelper?.songArtist = nextSong?.artist
                    Statified.currentSongHelper?.songId = nextSong?.songID as Long

                    /*updating the text views for title and artist name*/
                    updateTextViews(Statified.currentSongHelper?.songTitle as String, Statified.currentSongHelper?.songArtist as String)

                    Statified.mediaPlayer?.reset()
                    try {
                        Statified.mediaPlayer?.setDataSource(Statified.myActivity, Uri.parse(Statified.currentSongHelper?.songPath))
                        Statified.mediaPlayer?.prepare()
                        Statified.mediaPlayer?.start()

                        processInformation(Statified.mediaPlayer as MediaPlayer)

                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else {

                    /*If loop was OFF then normally play the next song*/
                    playNext("PlayNextNormal")
                    Statified.currentSongHelper?.isPlaying = true
                }
            }
            /*Here we check that if the song playing is a favorite, then we show a red colored heart indicating favorite else only the heart boundary
            * This action is performed whenever a new song is played, hence this will done in the playNext(), playPrevious() and onSongComplete() methods*/
            if (Statified.favoriteContent?.checkifIdExists(Statified.currentSongHelper?.songId?.toInt() as Int) as Boolean) {
                //fab?.setBackgroundResource(R.drawable.favorite_on)
                Statified.fab?.setImageDrawable(ContextCompat.getDrawable(Statified.myActivity, R.drawable.favorite_on))
            } else {
                //fab?.setBackgroundResource(R.drawable.favorite_off)
                Statified.fab?.setImageDrawable(ContextCompat.getDrawable(Statified.myActivity, R.drawable.favorite_off))
            }
        }

        /*Function to update the views of songs and their artist names*/
        fun updateTextViews(songTitle: String, songArtist: String) {
            Statified.songTitleView?.setText(songTitle)
            Statified.songArtistView?.setText(songArtist)
        }

        /*function used to update the time*/
        fun processInformation(mediaPlayer: MediaPlayer) {

            /*Obtaining the final time*/
            val finalTime = mediaPlayer.duration

            /*Obtaining the current position*/
            val startTime = mediaPlayer.currentPosition

            Statified.seekBar?.max = finalTime

            /*Here we format the time and set it to the start time text*/
            Statified.startTimeText?.setText(String.format("%d: %d",
                    TimeUnit.MILLISECONDS.toMinutes(startTime.toLong()),
                    TimeUnit.MILLISECONDS.toSeconds(startTime.toLong()) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(startTime.toLong())))
            )

            /*Similar to above is done for the end time text*/
            Statified.endTimeText?.setText(String.format("%d: %d",
                    TimeUnit.MILLISECONDS.toMinutes(finalTime.toLong()),
                    TimeUnit.MILLISECONDS.toSeconds(finalTime.toLong()) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(finalTime.toLong())))
            )

            /*Seekbar has been assigned this time so that it moves according to the time of song*/
            Statified.seekBar?.setProgress(startTime)

            /*Now this task is synced with the update song time obhect*/
            Handler().postDelayed(updateSongTime, 1000)
        }
        /*The playNext() function is used to play the next song*/
        fun playNext(check: String) {

            /*Let this one sit for a while, We'll explain this after the next section where we will be teaching to add the next and previous functionality*/
            if (check.equals("PlayNextNormal", true)) {
                Statified.currentPosition = currentPosition + 1
            } else if (check.equals("PlayNextLikeNormalShuffle", true)) {
                var randomObject = Random()
                var randomPosition = randomObject.nextInt(Statified.fetchSongs?.size?.plus(1) as Int)
                Statified.currentPosition = randomPosition
            }
            if (Statified.currentPosition == Statified.fetchSongs?.size) {
                Statified.currentPosition = 0
            }
            Statified.currentSongHelper?.isLoop = false
            var nextSong = Statified.fetchSongs?.get(Statified.currentPosition)
            Statified.currentSongHelper?.songPath = nextSong?.songData
            Statified.currentSongHelper?.songTitle = nextSong?.songTitle
            Statified.currentSongHelper?.songArtist = nextSong?.artist
            Statified.currentSongHelper?.songId = nextSong?.songID as Long

            /*updating the text views for title and artist name*/
            updateTextViews(Statified.currentSongHelper?.songTitle as String, Statified.currentSongHelper?.songArtist as String)

            Statified.mediaPlayer?.reset()
            try {
                Statified.mediaPlayer?.setDataSource(Statified.myActivity, Uri.parse(Statified.currentSongHelper?.songPath))
                Statified.mediaPlayer?.prepare()
                Statified.mediaPlayer?.start()

                processInformation(Statified.mediaPlayer as MediaPlayer)

            } catch (e: Exception) {
                e.printStackTrace()
            }
            /*Here we check that if the song playing is a favorite, then we show a red colored heart indicating favorite else only the heart boundary
            * This action is performed whenever a new song is played, hence this will done in the playNext(), playPrevious() and onSongComplete() methods*/
            if (Statified.favoriteContent?.checkifIdExists(Statified.currentSongHelper?.songId?.toInt() as Int) as Boolean) {
                //fab?.setBackgroundResource(R.drawable.favorite_on)
                Statified.fab?.setImageDrawable(ContextCompat.getDrawable(Statified.myActivity, R.drawable.favorite_on))
            } else {
                //fab?.setBackgroundResource(R.drawable.favorite_off)
                Statified.fab?.setImageDrawable(ContextCompat.getDrawable(Statified.myActivity, R.drawable.favorite_off))
            }
        }
    }

    /*Similar onCreateView() method of the fragment, which we used for the MainScreenFragment*/
    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater!!.inflate(R.layout.fragment_song_playing, container, false)

        /*Linking views with their ids*/
        Statified.seekBar = view?.findViewById(R.id.seekBar)
        Statified.startTimeText = view?.findViewById(R.id.startTime)
        Statified.endTimeText = view?.findViewById(R.id.endTime)
        Statified.playPauseImageButton = view?.findViewById(R.id.playPauseButton)
        Statified.nextImageButton = view?.findViewById(R.id.nextButton)
        Statified.previousImageButton = view?.findViewById(R.id.prevoiusButton)
        Statified.loopImageButton = view?.findViewById(R.id.loopButton)
        Statified.shuffleImageButton = view?.findViewById(R.id.shuffleButton)
        Statified.songArtistView = view?.findViewById(R.id.songArtist)
        Statified.songTitleView = view?.findViewById(R.id.songTitle)
        Statified.glView = view?.findViewById(R.id.visualizer_view)
        Statified.fab = view?.findViewById(R.id.favoriteIcon)
        Statified.fab?.alpha = 0.8f

        return view
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Statified.audioVisualization = Statified.glView as AudioVisualization
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        Statified.myActivity = context as Activity
    }

    override fun onAttach(activity: Activity?) {
        super.onAttach(activity)
        Statified.myActivity = activity
    }

    override fun onResume() {
        super.onResume()
        Statified.audioVisualization?.onResume()
    }

    override fun onPause() {
        super.onPause()
        Statified.audioVisualization?.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        Statified.audioVisualization?.release()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        /*Initialising the database*/
        Statified.favoriteContent = EchoDatabase(Statified.myActivity)
        Statified.currentSongHelper = CurrentSongHelper()
        Statified.currentSongHelper?.isPlaying = true
        Statified.currentSongHelper?.isLoop = false
        Statified.currentSongHelper?.isShuffle = false

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
            Statified.currentPosition = arguments.getInt("songPosition")
            Statified.fetchSongs = arguments.getParcelableArrayList("songData")

            /*Now store the song details to the current song helper object so that they can be used later*/
            Statified.currentSongHelper?.songPath = path
            Statified.currentSongHelper?.songTitle = _songTitle
            Statified.currentSongHelper?.songArtist = _songArtist
            Statified.currentSongHelper?.songId = songId
            Statified.currentSongHelper?.currentPosition = Statified.currentPosition

        } catch (e: Exception) {
            e.printStackTrace()
        }

        var fromFavBottomBar = arguments.get("FavBottomBar") as? String
        if(fromFavBottomBar != null){
            Statified.mediaPlayer = FavoriteFragment.Statified.mediaPlayer
        } else {

            /*here we initialise the media player object*/
            Statified.mediaPlayer = MediaPlayer()

            /*here we tell the media player object that we would be streaming the music*/
            Statified.mediaPlayer?.setAudioStreamType(AudioManager.STREAM_MUSIC)

            /*Here also we use the error-handling as the path we sent may return a null object*/
            try {

                /*The data source set the song to the media player object*/
                Statified.mediaPlayer?.setDataSource(Statified.myActivity, Uri.parse(path))

                /*Before plaing the music we prepare the media player for playback*/
                Statified.mediaPlayer?.prepare()

            } catch (e: Exception) {
                e.printStackTrace()
            }

            /*If all of the above goes well we start the music using the start() method*/
            Statified.mediaPlayer?.start()
        }

        processInformation(Statified.mediaPlayer as MediaPlayer)

        if (Statified.currentSongHelper?.isPlaying as Boolean) {
            Statified.playPauseImageButton?.setBackgroundResource(R.drawable.pause_icon)
        } else {
            Statified.playPauseImageButton?.setBackgroundResource(R.drawable.play_icon)
        }
        Statified.mediaPlayer?.setOnCompletionListener {
            onSongComplete()
        }
        clickHandler()

        var visualizationHandler = DbmHandler.Factory.newVisualizerHandler(Statified.myActivity as Context, 0)
        Statified.audioVisualization?.linkTo(visualizationHandler)

        /*Now we want that when if user has turned shuffle or loop ON, then these settings should persist even if the app is restarted after closing
        * This is done with the help of Shared Preferences
        * Shared preferences are capable of storing small amount of data in the form of key-value pair*/

        /*Here we initialize the preferences for shuffle in a private mode
        * Private mode is chosen so that so other app us able to read the preferences apart from our app*/
        var prefsForShuffle = Statified.myActivity?.getSharedPreferences(Staticated.MY_PREFS_SHUFFLE, Context.MODE_PRIVATE)

        /*Here we extract the value of preferences and check if shuffle was ON or not*/
        var isShuffleAllowed = prefsForShuffle?.getBoolean("feature", false)
        if (isShuffleAllowed as Boolean) {

            /*if shuffle was found activated, then we change the icon color and tun loop OFF*/
            Statified.currentSongHelper?.isShuffle = true
            Statified.currentSongHelper?.isLoop = false
            Statified.shuffleImageButton?.setBackgroundResource(R.drawable.shuffle_icon)
            Statified.loopImageButton?.setBackgroundResource(R.drawable.loop_white_icon)
        } else {
            /*Else default is set*/
            Statified.currentSongHelper?.isShuffle = false
            Statified.shuffleImageButton?.setBackgroundResource(R.drawable.shuffle_white_icon)
        }
        /*Similar to the shuffle we check the value for loop activation*/
        var prefsForLoop = Statified.myActivity?.getSharedPreferences(Staticated.MY_PREFS_LOOP, Context.MODE_PRIVATE)

        /*Here we extract the value of preferences and check if loop was ON or not*/
        var isLoopAllowed = prefsForLoop?.getBoolean("feature", false)
        if (isLoopAllowed as Boolean) {

            /*If loop was activated we change the icon color and shuffle is turned OFF */
            Statified.currentSongHelper?.isShuffle = false
            Statified.currentSongHelper?.isLoop = true
            Statified.shuffleImageButton?.setBackgroundResource(R.drawable.shuffle_white_icon)
            Statified.loopImageButton?.setBackgroundResource(R.drawable.loop_icon)
        } else {

            /*Else defaults are used*/
            Statified.loopImageButton?.setBackgroundResource(R.drawable.loop_white_icon)
            Statified.currentSongHelper?.isLoop = false
        }
        /*Here we check that if the song playing is a favorite, then we show a red colored heart indicating favorite else only the heart boundary
        * This action is performed whenever a new song is played, hence this will done in the playNext(), playPrevious() and onSongComplete() methods*/
        if (Statified.favoriteContent?.checkifIdExists(Statified.currentSongHelper?.songId?.toInt() as Int) as Boolean) {
            //fab?.setBackgroundResource(R.drawable.favorite_on)
            Statified.fab?.setImageDrawable(ContextCompat.getDrawable(Statified.myActivity, R.drawable.favorite_on))
        } else {
            //fab?.setBackgroundResource(R.drawable.favorite_off)
            Statified.fab?.setImageDrawable(ContextCompat.getDrawable(Statified.myActivity, R.drawable.favorite_off))
        }
    }

    /*A new click handler function is created to handle all the click functions in the song playing fragment*/
    fun clickHandler() {

        Statified.fab?.setOnClickListener({
            /*Here we check that if the song playing is a favorite, then we show a red colored heart indicating favorite else only the heart boundary
        * This action is performed whenever a new song is played, hence this will done in the playNext(), playPrevious() and onSongComplete() methods*/
            if (Statified.favoriteContent?.checkifIdExists(Statified.currentSongHelper?.songId?.toInt() as Int) as Boolean) {
                //fab?.setBackgroundResource(R.drawable.favorite_on)
                Statified.fab?.setImageDrawable(ContextCompat.getDrawable(Statified.myActivity, R.drawable.favorite_off))
                Statified.favoriteContent?.deleteFavourite(Statified.currentSongHelper?.songId?.toInt() as Int)

                /*Toast is prompt message at the bottom of screen indicating that an action has been performed*/
                Toast.makeText(Statified.myActivity, "Removed from Favorites", Toast.LENGTH_SHORT).show()
            } else {
                //fab?.setBackgroundResource(R.drawable.favorite_off)
                Statified.fab?.setImageDrawable(ContextCompat.getDrawable(Statified.myActivity, R.drawable.favorite_on))
                /*If the song was not a favorite, we then add it to the favorites using the method we made in our database*/
                Statified.favoriteContent?.storeAsFavorite(Statified.currentSongHelper?.songId?.toInt(), Statified.currentSongHelper?.songArtist,
                        Statified.currentSongHelper?.songTitle, Statified.currentSongHelper?.songPath)
                Toast.makeText(Statified.myActivity, "Added to Favorites", Toast.LENGTH_SHORT).show()
            }
        })

        /*The implementation will be taught in the coming topics*/
        Statified.shuffleImageButton?.setOnClickListener({

            /*Initializing the shared preferences in private mode
            * edit() used so that we can overwrite the preferences*/
            var editorShuffle = Statified.myActivity?.getSharedPreferences(Staticated.MY_PREFS_SHUFFLE, Context.MODE_PRIVATE)?.edit()
            var editorLoop = Statified.myActivity?.getSharedPreferences(Staticated.MY_PREFS_LOOP, Context.MODE_PRIVATE)?.edit()

            if (Statified.currentSongHelper?.isShuffle as Boolean) {
                Statified.shuffleImageButton?.setBackgroundResource(R.drawable.shuffle_white_icon)
                Statified.currentSongHelper?.isShuffle = false
                /*If shuffle was activated previously, then we deactivate it*/
                /*The putBoolean() method is used for saving the boolean value against the key which is feature here*/

                /*Now the preferences agains the block Shuffle feature will have a key: feature and its value: false*/
                editorShuffle?.putBoolean("feature", false)
                editorShuffle?.apply()

            } else {
                Statified.currentSongHelper?.isShuffle = true
                Statified.currentSongHelper?.isLoop = false
                Statified.shuffleImageButton?.setBackgroundResource(R.drawable.shuffle_icon)
                Statified.loopImageButton?.setBackgroundResource(R.drawable.loop_white_icon)

                /*Else shuffle is activated and if loop was activated then loop is deactivated*/
                editorShuffle?.putBoolean("feature", true)
                editorShuffle?.apply()


                /*Similar to shuffle, the loop feature has a key:feature and its value:false*/
                editorLoop?.putBoolean("feature", false)
                editorLoop?.apply()
            }
        })
        Statified.nextImageButton?.setOnClickListener({
            Statified.currentSongHelper?.isPlaying = true
            if (Statified.currentSongHelper?.isShuffle as Boolean) {
                playNext("PlayNextLikeNormalShuffle")
            } else {
                playNext("PlayNextNormal")
            }
        })
        Statified.previousImageButton?.setOnClickListener({
            /*We set the player to be playing by setting isPlaying to be true*/
            Statified.currentSongHelper?.isPlaying = true

            /*First we check if the loop is on or not*/
            if (Statified.currentSongHelper?.isLoop as Boolean) {

                /*If the loop was on we turn it off*/
                Statified.loopImageButton?.setBackgroundResource(R.drawable.loop_white_icon)
            }

            /*After all of the above is done we then play the previous song using the playPrevious() function*/
            playPrevious()
        })
        Statified.loopImageButton?.setOnClickListener({

            /*The operation on preferences is completely analogous to shuffle, no addition is there*/
            var editorShuffle = Statified.myActivity?.getSharedPreferences(Staticated.MY_PREFS_SHUFFLE, Context.MODE_PRIVATE)?.edit()
            var editorLoop = Statified.myActivity?.getSharedPreferences(Staticated.MY_PREFS_LOOP, Context.MODE_PRIVATE)?.edit()


            if (Statified.currentSongHelper?.isLoop as Boolean) {                                 /*if loop was enabled, we turn it off and vice versa*/
                Statified.currentSongHelper?.isLoop = false                                       /*Making the isLoop false*/
                Statified.loopImageButton?.setBackgroundResource(R.drawable.loop_white_icon)      /*We change the color of the icon*/
                editorLoop?.putBoolean("feature", false)
                editorLoop?.apply()
            } else {

                /*If loop was not enabled when tapped, we enable if and make the isLoop to true*/
                Statified.currentSongHelper?.isLoop = true

                /*Loop and shuffle won't work together so we put shuffle false irrespectve of the whether it was on or not*/
                Statified.currentSongHelper?.isShuffle = false

                /*Loop button color changed to mark it ON*/
                Statified.loopImageButton?.setBackgroundResource(R.drawable.loop_icon)

                /*Changing the shuffle button to white, no matter which color it was earlier*/
                Statified.shuffleImageButton?.setBackgroundResource(R.drawable.shuffle_white_icon)

                editorShuffle?.putBoolean("feature", false)
                editorShuffle?.apply()
                editorLoop?.putBoolean("feature", true)
                editorLoop?.apply()
            }
        })

        /*Here we handle the click event on the play/pause button*/
        Statified.playPauseImageButton?.setOnClickListener({

            /*if the song is already playing and then play/pause button is tapped
            * then we pause the media player and also change the button to play button*/
            if (Statified.mediaPlayer?.isPlaying as Boolean) {
                Statified.mediaPlayer?.pause()
                Statified.currentSongHelper?.isPlaying = false
                Statified.playPauseImageButton?.setBackgroundResource(R.drawable.play_icon)

                /*If the song was not playing the, we start the music player and
                * change the image to pause icon*/
            } else {
                Statified.mediaPlayer?.start()

                processInformation(Statified.mediaPlayer as MediaPlayer)

                Statified.currentSongHelper?.isPlaying = true
                Statified.playPauseImageButton?.setBackgroundResource(R.drawable.pause_icon)
            }
        })
    }

    /*The function playPrevious() is used to play the previous song again*/
    fun playPrevious() {

        /*Decreasing the current position by 1 to get the position of the previous song*/
        Statified.currentPosition = Statified.currentPosition - 1

        /*If the current position becomes less than 1, we make it 0 as there is no index as -1*/
        if (Statified.currentPosition == -1) {
            Statified.currentPosition = 0
        }
        if (Statified.currentSongHelper?.isPlaying as Boolean) {
            Statified.playPauseImageButton?.setBackgroundResource(R.drawable.pause_icon)
        } else {
            Statified.playPauseImageButton?.setBackgroundResource(R.drawable.play_icon)
        }
        Statified.currentSongHelper?.isLoop = false

        /*Similar to the playNext() function defined above*/
        var nextSong = Statified.fetchSongs?.get(Statified.currentPosition)
        Statified.currentSongHelper?.songPath = nextSong?.songData
        Statified.currentSongHelper?.songTitle = nextSong?.songTitle
        Statified.currentSongHelper?.songArtist = nextSong?.artist
        Statified.currentSongHelper?.songId = nextSong?.songID as Long

        /*updating the text views for title and artist name*/
        updateTextViews(Statified.currentSongHelper?.songTitle as String, Statified.currentSongHelper?.songArtist as String)

        Statified.mediaPlayer?.reset()
        try {
            Statified.mediaPlayer?.setDataSource(Statified.myActivity, Uri.parse(Statified.currentSongHelper?.songPath))
            Statified.mediaPlayer?.prepare()
            Statified.mediaPlayer?.start()

            processInformation(Statified.mediaPlayer as MediaPlayer)

        } catch (e: Exception) {
            e.printStackTrace()
        }
        /*Here we check that if the song playing is a favorite, then we show a red colored heart indicating favorite else only the heart boundary
        * This action is performed whenever a new song is played, hence this will done in the playNext(), playPrevious() and onSongComplete() methods*/
        if (Statified.favoriteContent?.checkifIdExists(Statified.currentSongHelper?.songId?.toInt() as Int) as Boolean) {
            //fab?.setBackgroundResource(R.drawable.favorite_on)
            Statified.fab?.setImageDrawable(ContextCompat.getDrawable(Statified.myActivity, R.drawable.favorite_on))
        } else {
            //fab?.setBackgroundResource(R.drawable.favorite_off)
            Statified.fab?.setImageDrawable(ContextCompat.getDrawable(Statified.myActivity, R.drawable.favorite_off))
        }
    }
}

