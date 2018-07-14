package com.example.nouman.echo.databases

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.nouman.echo.Songs
import com.example.nouman.echo.databases.EchoDatabase.Staticated.COLUMN_SONG_ARTIST
import com.example.nouman.echo.databases.EchoDatabase.Staticated.COLUMN_SONG_PATH
import com.example.nouman.echo.databases.EchoDatabase.Staticated.COLUMN_SONG_TITLE
import com.example.nouman.echo.databases.EchoDatabase.Staticated.DB_NAME
import com.example.nouman.echo.databases.EchoDatabase.Staticated.DB_VERSION
import com.example.nouman.echo.databases.EchoDatabase.Staticated.TABLE_NAME

class EchoDatabase : SQLiteOpenHelper {
    var _songList = ArrayList<Songs>()

    object Staticated {
        /*Let's define some params for our database
    * All the below params are case-sensitive and should be used with the same case*/

        /*Name of the table*/
        val TABLE_NAME = "FavoriteTable"

        /*Name of column 1*/
        val COLUMN_ID = "SongID"

        /*Name of column 2*/
        val COLUMN_SONG_TITLE = "SongTitle"

        /*Name of column 3*/
        val COLUMN_SONG_ARTIST = "SongArtist"

        /*Name of column 4*/
        val COLUMN_SONG_PATH = "SongPath"
        /*Database Name*/
        val DB_NAME = "FavoriteDatabase"
        var DB_VERSION = 1
    }

    /*This function is called when the application first creates the database
    * If already a table with the same name is present in the database, then this method is skipped*/
    override fun onCreate(sqliteDatabase: SQLiteDatabase?) {

        /*In-order to perform any function in our database we use SQL queries
        * These queries are pre-defined statements similar to english statements which perform any action in the database*/
        /*The query here is used to create the table
        * The query is :
        * "CREATE TABLE FavoriteTable (
        *  SongsID INTEGER,
        *  SongArtist STRING,
        *  SongTitle STRING,
        *  SongPath STRING);"  */
        sqliteDatabase?.execSQL("CREATE TABLE " + Staticated.TABLE_NAME + "( " + Staticated.COLUMN_ID +
                " INTEGER," + Staticated.COLUMN_SONG_ARTIST + " STRING," + Staticated.COLUMN_SONG_TITLE + " STRING,"
                + Staticated.COLUMN_SONG_PATH + " STRING);")
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    constructor(context: Context?, name: String?, factory: SQLiteDatabase.CursorFactory?, version: Int) : super(context, name, factory, version) {}
    constructor(context: Context?) : super(context, Staticated.DB_NAME, null, Staticated.DB_VERSION) {}

    /*As the name suggests, this function is used to store the songs as favorites*/
    fun storeAsFavorite(id: Int?, artist: String?, songTitle: String?, path: String?) {

        /*The function writableDatabase is used to open the db for editing so that changes can be made to the database*/
        val db = this.writableDatabase

        /*Here we define the Content Values which store the values which are pushed into the database*/
        val contentValues = ContentValues()

        /*The .put() function is used for adding the values to the content values object*/
        contentValues.put(Staticated.COLUMN_ID, id)
        contentValues.put(Staticated.COLUMN_SONG_ARTIST, artist)
        contentValues.put(Staticated.COLUMN_SONG_TITLE, songTitle)
        contentValues.put(Staticated.COLUMN_SONG_PATH, path)

        /*Here we use the insert function to insert the values into the table whose name is specified using the TABLE_NAME
        * and the values which are added are the content values*/
        db.insert(Staticated.TABLE_NAME, null, contentValues)

        /*After performing the db actions, we are required to close the database in-order to prevent any data leakage and saving the resources*/
        db.close()
    }

    /*This method asks the database for the list of Songs stored as favorite*/
    fun queryDBList(): ArrayList<Songs>? {

        /*Here a try-catch block is used to handle the exception as no songs in the database can result in null-pointer exception*/

        try {
            val db = this.readableDatabase

            /*The SQL query used for obtaining the songs is :
            * SELECT * FROM FavoriteTable
            * The query returns all the items present in the table*/
            val query_params = "SELECT * FROM " + Staticated.TABLE_NAME
            var cSor = db.rawQuery(query_params, null)

            /*The cSor stores the result obtained from the database
            * The function moveToFirst() checks if there are any entries or not*/
            if (cSor.moveToFirst()) {

                /*If 1 or more rows are returned then we store all the entries into the array list _songList*/
                do {

                    var _id = cSor.getInt(cSor.getColumnIndexOrThrow(Staticated.COLUMN_ID))
                    var _artist = cSor.getString(cSor.getColumnIndexOrThrow(Staticated.COLUMN_SONG_ARTIST))
                    var _title = cSor.getString(cSor.getColumnIndexOrThrow(Staticated.COLUMN_SONG_TITLE))
                    var _songPath = cSor.getString(cSor.getColumnIndexOrThrow(Staticated.COLUMN_SONG_PATH))
                    _songList.add(Songs(_id.toLong(), _title, _artist, _songPath, 0))
                }

                /*This task is performed till there are items present*/
                while (cSor.moveToNext())
            }

            /*Otherwise null is returned*/
            else {
                return null
            }
        }

        /*If there was any exception then it is handled by this*/
        catch (e: Exception) {
            e.printStackTrace()
        }

        /*Finally we return the songList which contains the songs present inside the database*/
        return _songList
    }

    /*This function is created for checking whether a particular song is a favorite or not*/
    fun checkifIdExists(_id: Int): Boolean {

        /*Random id which does not exist
        * We know that this id can never exist as the song id cannot be less than 0*/
        var storeId = -1090
        val db = this.readableDatabase

        /*The query for checking the if id is present or not is
        * SELECT * FROM FavoriteTable WHERE SongID = <id_of_our_song>*/
        val query_params = "SELECT * FROM " + Staticated.TABLE_NAME + " WHERE SongID = '$_id'"
        val cSor = db.rawQuery(query_params, null)
        if (cSor.moveToFirst()) {
            do {

                /*Storing the song id into the variable storeId*/
                storeId = cSor.getInt(cSor.getColumnIndexOrThrow(Staticated.COLUMN_ID))
            } while (cSor.moveToNext())
        } else {
            return false
        }

        /*Here we need to return a boolean value i.e. true or false
        * Hence we check if the store id is not equal to -1090 then we return true, else we return false*/
        return storeId != -1090
    }

    /*This function is used to delete the songs from the favorite if the user the user removes any song from the favorite list*/
    fun deleteFavourite(_id: Int) {
        val db = this.writableDatabase

        /*The delete query is used to perform the delete function*/
        db.delete(TABLE_NAME, Staticated.COLUMN_ID + " = " + _id, null)

        /*Here is also we close the database connection
        * Note that we only close the database whenever we open in writable mode*/
        db.close()
    }

    /*The function checkSize() is used to calculate whether we have any song as favorite or not*/
    fun checkSize(): Int {
        var counter = 0
        val db = this.readableDatabase
        var query_params = "SELECT * FROM " + TABLE_NAME
        /*Here the cursor(cSor) stores the entries returned by the database*/
        val cSor = db.rawQuery(query_params, null)
        /*We add 1 to the counter for every entry*/
        if (cSor.moveToFirst()) {
            do {
                counter = counter + 1
            } while (cSor.moveToNext())
        } else {
            return 0
        }

        /*returning the counter will return the number of elements in the database*/
        return counter
    }
}