package com.droidroid.PM2


//import java.util.concurrent.Executors

import android.R.drawable
import android.content.res.Configuration
import android.net.Uri
import com.google.analytics.tracking.android.EasyTracker

//import android.animation.{AnimatorListenerAdapter, Animator}
import android.app.Activity
import android.content._
import android.graphics.{Color, Point}
import android.os.{AsyncTask, Bundle, IBinder}
import android.support.v4.content.LocalBroadcastManager
import android.view._
import android.widget.AdapterView.OnItemClickListener
import android.widget._

import scala.collection.mutable

//import android.speech.{RecognitionListener, RecognizerIntent, SpeechRecognizer}
//import android.util.{DisplayMetrics, Log}
import java.util

import android.graphics.Paint.Align
import com.google.gson.Gson
import org.achartengine.ChartFactory
import org.achartengine.chart.BarChart.Type
import org.achartengine.model.{CategorySeries, SeriesSelection, XYMultipleSeriesDataset, XYSeries}
import org.achartengine.renderer.{XYMultipleSeriesRenderer, XYSeriesRenderer}

import scala.Array._
import scala.Predef._
import scala.concurrent.ops._

//import scala.concurrent.{ExecutionContext, Await, future}
//import scala.concurrent.duration.Duration



/* ed@ryer.ru, ed@droidroid.com */

/*
PM Activity 8th Jan 14-Feb 14, 18 Jul - 30 Aug 14 maxed out
*/


/*
08-03 20:42:19.936  30194-30194/com.droidroid.PM2 D/NDK_AndroidNDK1Activity﹕ NDK:LC: EVAL6 outs5 nDeck is [8423187] [4204049] - [134253349] [2114829] [33589533] - [0] [0] vresult=[5104]
The 66AQ2 error occurs because of a 0 fed into the alg during a loop in the NDK!
 */


/*
24-08-14
as qh kc qd td 7d 8h
changing 7d -> 7h
still leaves 8h in best hand!
only wheel turn turn to river
back further no issues?
 */


class PrimaryActivity extends Activity {

  final val TAG:String = "PrimaryActivity_"

  final val VERSION:String = "Pro"

  final val IGNOREDCARD:Int = 5104 //66AQ2 issue

  private var scrolling: Boolean = false
  var gamePlayers = 0

  var prefs: SharedPreferences = null


  private val cry = new Crypto_htable()
  private val cryma3 = new Crypto_maxordering()
  private val cryma6 = new Crypto_maxordering()
  private val cryma10 = new Crypto_maxordering()

  //qa
  private var quickActionV:QuickAction=null
  private var quickActionH:QuickAction=null

  //fade
  var mFlipperView:View=null
  var mFlipperView2:View=null
  var mShortAnimationDuration:Int=0
  var fl:OutsTable=null

  //screen defaults to lowest
  var device_width:Int =1024
  var device_height:Int=768

  //to allow globally for qa handlers
  var adapt:ListAdapter =null

  //temp cards
  var tempMiniCards:Array[Int] = Array.fill[Int](3)(0)

  //intents
  var service_intent:Intent=null

  //binder stuff
  var mService:EquityBroadcastServiceWrapper=null
  var mBound:Int = 0 //binder count

  //decode
  var ek:String=""
  var zulu71:Short=0
  final val zulu52:Short=52 //crypto


  //we load the Alg Library (written in C and problematic to convert due to low level operations, also the C is significantly faster for our computations"
  System.loadLibrary("cEngine")        // --- Native methods
  @native def coreEngine(actor:String, actorReturn:String, c1:Int,c2:Int,c3:Int,c4:Int,c5:Int,c6:Int,c7:Int): Array[Int]  //return array even sometimes we wish a scalar

  System.loadLibrary("gCx")        // --- Native methods
  @native def gC(auth:String): String



  override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)

    prefs = this.getSharedPreferences("com.droidroid.PM2", Context.MODE_PRIVATE)


    //onCreate delete any previous state - only used for PS-R
    spawn {
    prefs.edit().putString("bun0", "").apply()
    prefs.edit().putString("bun1", "").apply()
    prefs.edit().putString("bun2", "").apply()
    }

    //register our service
    //service_intent  = new Intent(this, classOf[EquityBroadcastServiceWrapper])

    //14-aug-14 binder
    //Log.d(TAG,"a binder bindService "+mBound)
    val mServiceIntent:Intent = new Intent(this, classOf[EquityBroadcastServiceWrapper])
    bindService(mServiceIntent, mConnection, Context.BIND_AUTO_CREATE)



    //screen size
    val display:Display  = getWindowManager().getDefaultDisplay()
    val size:Point  = new Point()
    display.getSize(size)
    device_width= size.x
    device_height= size.y
    //ObjectUtils.showToast("x="+device_width+" y="+device_height,this)


    //speech support
    //TODO improve dramatically is almost useless
    //val sr: SpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(getApplicationContext())
    //val mrl:MyRecognitionListener = new MyRecognitionListener()
    //sr.setRecognitionListener(this)


    ObjectUtils.showToast("act_ onCreate()",getBaseContext)
    //decrypt resources once off UI but still UI thread...

    zulu71 = 71 //crypto

  /* 29-7-14 moved off to inner class asyncTask */
   val doui:DoOffUI  = new DoOffUI()
   doui.execute(this)

    /*
    // single threaded execution context
    implicit val context = ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor())

    val f = future {

      try {

        cry.decrypt(this, R.raw.mash) //htable
        cryma3.decrypt(this, R.raw.baz) //3Max
        cryma6.decrypt(this, R.raw.las6y) //6Max
        cryma10.decrypt(this, R.raw.las1y0) //10Max
      }
      catch {
        case _:Throwable => Log.d(TAG," future oopsy ")
      }
    }


    f.onComplete { result => Log.d(TAG,"completed thread "+f) }
  */

    //Remove title bar
    this.requestWindowFeature(Window.FEATURE_NO_TITLE)
    //Remove notification bar
    this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)

    //inflate
    setContentView(R.layout.main) //our main layout

    val logoView = this.findViewById(R.id.imageViewLogo).asInstanceOf[ImageView]
    if (VERSION=="Pro") {
      logoView.setBackgroundResource(R.drawable.pm_logo_a)
    }
    else logoView.setBackgroundResource(R.drawable.pm_logo_a_free)



    //place the initial freeze view
    initialFreezeViews

    //status lines
    writeBigStatusLine("Enter pre-flop cards..")
    writeTopStatusLine()


    //flipping of views hide one
    mFlipperView = this.findViewById(R.id.flipper).asInstanceOf[LinearLayout]
    mFlipperView2 = this.findViewById(R.id.flipper2).asInstanceOf[FrameLayout]
    mFlipperView2.setVisibility(View.GONE)

    //lock switch until flop
    mFlipperView.setEnabled(false)
    mFlipperView2.setEnabled(false)

    this.findViewById(R.id.mypos).asInstanceOf[FrameLayout].setAlpha(0.3f)


    fl = new OutsTable

    // Retrieve and cache the system's default "short" animation time.
    mShortAnimationDuration = getResources().getInteger(android.R.integer.config_shortAnimTime)



    //set the hero best hand opacity
    val civ1:View =  findViewById(R.id.cardView1)
    val civ2:View =  findViewById(R.id.cardView2)
    val civ3:View =  findViewById(R.id.cardView3)
    val civ4:View =  findViewById(R.id.cardView4)
    val civ5:View =  findViewById(R.id.cardView5)
    val civ6:View =  findViewById(R.id.cardView6)
    val civ7:View =  findViewById(R.id.cardView7)

    civ1.setAlpha(0.5f)
    civ2.setAlpha(0.5f)
    civ3.setAlpha(0.5f)
    civ4.setAlpha(0.5f)
    civ5.setAlpha(0.5f)
    civ6.setAlpha(0.5f)
    civ7.setAlpha(0.5f)

    //pf1 layout placement

    val wv1: WheelView = findViewById(R.id.card_display_1).asInstanceOf[WheelView]
    //pf1.setVisibleItems(8)
    val wva = new CardAdapter(this, ObjectUtils.currentDeck,ObjectUtils.currDeckImages)
    wva.setTextSize(25)
    wv1.setViewAdapter(wva)
    wv1.setCurrentItem(52)


    //pf2 layout placement
    val wv2: WheelView = findViewById(R.id.card_display_2).asInstanceOf[WheelView]
    //pf2.setVisibleItems(8)
    val wvb = new CardAdapter(this, ObjectUtils.currentDeck,ObjectUtils.currDeckImages)
    wvb.setTextSize(25)
    wv2.setViewAdapter(wvb)
    //disable wheel until use
    wv2.setCurrentItem(52)
    wv2.setAlpha(0.5f)
    wv2.setEnabled(false)


    //pf3 layout placement
    val wv3: WheelView = findViewById(R.id.card_display_3).asInstanceOf[WheelView]
    //pf2.setVisibleItems(8)
    val wvc = new CardAdapter(this, ObjectUtils.currentDeck,ObjectUtils.currDeckImages)
    wvc.setTextSize(25)
    wv3.setViewAdapter(wvc)
    wv3.setCurrentItem(52)
    //disable wheel until use
    wv3.setAlpha(0.3f)
    wv3.setEnabled(false)


    //pf4 layout placement
    val wv4: WheelView = findViewById(R.id.card_display_4).asInstanceOf[WheelView]
    //pf2.setVisibleItems(8)
    val wvd = new CardAdapter(this, ObjectUtils.currentDeck,ObjectUtils.currDeckImages)
    wvd.setTextSize(25)
    wv4.setViewAdapter(wvd)
    wv4.setCurrentItem(52)
    //disable wheel until use
    wv4.setAlpha(0.3f)
    wv4.setEnabled(false)

    //pf5 layout placement
    val wv5: WheelView = findViewById(R.id.card_display_5).asInstanceOf[WheelView]
    //pf2.setVisibleItems(8)
    val pfe = new CardAdapter(this, ObjectUtils.currentDeck,ObjectUtils.currDeckImages)
    pfe.setTextSize(25)
    wv5.setViewAdapter(pfe)
    wv5.setCurrentItem(52)
    //disable wheel until use
    wv5.setAlpha(0.3f)
    wv5.setEnabled(false)


    //pf6 layout placement
    val wv6: WheelView = findViewById(R.id.card_display_6).asInstanceOf[WheelView]
    //pf2.setVisibleItems(8)
    val wvf = new CardAdapter(this, ObjectUtils.currentDeck,ObjectUtils.currDeckImages)
    wvf.setTextSize(25)
    wv6.setViewAdapter(wvf)
    wv6.setCurrentItem(52)
    //disable wheel until use
    wv6.setAlpha(0.3f)
    wv6.setEnabled(false)



    //pf7 layout placement
    val wv7: WheelView = findViewById(R.id.card_display_7).asInstanceOf[WheelView]
    //pf2.setVisibleItems(8)
    val wvg = new CardAdapter(this, ObjectUtils.currentDeck,ObjectUtils.currDeckImages)
    wvg.setTextSize(25)
    wv7.setViewAdapter(wvg)
    wv7.setCurrentItem(52)
    //disable wheel until use
    wv7.setAlpha(0.3f)
    wv7.setEnabled(false)

    //for status dimming on less than flop
    val lsl:LinearLayout  = findViewById(R.id.status_view).asInstanceOf[LinearLayout]
    val lsbl:LinearLayout  = findViewById(R.id.statusBox).asInstanceOf[LinearLayout]




    //finally remove the red back
    //ObjectUtils.removeRedBack()


    //module: Listeners for the Scrollers
    //Listener 1

    wv1.addChangingListener(new OnWheelChangedListener {
    def onChanged(wheel: WheelView, oldValue: Int, newValue: Int) {

      //ObjectUtils.showToast("onChanged "+newValue,getBaseContext)
      if (!scrolling) {
      }
    }
  })
    wv1.addScrollingListener(new OnWheelScrollListener {
    def onScrollingStarted(wheel: WheelView) {
      scrolling = true
      //ObjectUtils.showToast("onScrollingStarted",getBaseContext)
    }

    def onScrollingFinished(wheel: WheelView) {
      scrolling = false
      //ObjectUtils.showToast("onScrollingFinished",getBaseContext)
      findViewById(R.id.flop_list).asInstanceOf[LinearLayout].setBackgroundDrawable(null)

      //10-aug-14 not working TODO reset alpha in case of a repeat
      findViewById(R.id.flop_list).asInstanceOf[TableRow].setAlpha(0.85f)


      //ObjectUtils.showToast("onScrollingFinished "+wheel.getCurrentItem,getBaseContext)
      //updateWheelCardMid(wv2)

      updateWheelRotor(2,wheel.getCurrentItem)
      //wv2.setEnabled(true) //re-enable next wheel
      //we don't re-enable but discard this wheel as we can't setCurrentItem as it uses it's original value before disabled

      val hiStatLineH=findViewById(R.id.MiniStatusBar).asInstanceOf[TextView]
      hiStatLineH.setText("Enter preflop cards...")
      //updateWheelRotor sets the new value

      //09-aug-14 can replay now not needing a reset
      //reset flop if coming back
      wv2.setCurrentItem(52)
      wv3.setCurrentItem(52)
      wv4.setCurrentItem(52)
      wv5.setCurrentItem(52)
      wv6.setCurrentItem(52)
      wv7.setCurrentItem(52)


      //reset the mini status
      statusCardsReset(1)


      //10-aug-14 add the card to the mini card list
      findViewById(R.id.cardView1).asInstanceOf[ImageView].setImageResource(ObjectUtils.refDeckImages(wv1.getCurrentItem))


      /*
      findViewById(R.id.cardView1).asInstanceOf[ImageView].setImageResource(R.drawable.red_back)
      findViewById(R.id.cardView2).asInstanceOf[ImageView].setImageResource(R.drawable.red_back)
      findViewById(R.id.cardView3).asInstanceOf[ImageView].setImageResource(R.drawable.red_back)
      findViewById(R.id.cardView4).asInstanceOf[ImageView].setImageResource(R.drawable.red_back)
      findViewById(R.id.cardView5).asInstanceOf[ImageView].setImageResource(R.drawable.red_back)

      findViewById(R.id.cardView6).asInstanceOf[ImageView].setImageResource(R.drawable.red_back)
      findViewById(R.id.cardView7).asInstanceOf[ImageView].setImageResource(R.drawable.red_back)

      */

      //block ahead
      wv3.setEnabled(false)
      wv4.setEnabled(false)
      wv5.setEnabled(false)
      wv6.setEnabled(false)
      wv7.setEnabled(false)

      //and alpha
      wv3.setAlpha(0.3f)
      wv4.setAlpha(0.3f)
      wv5.setAlpha(0.3f)
      wv6.setAlpha(0.3f)
      wv7.setAlpha(0.3f)



      //and any alpha
      lsl.setAlpha(0.4f)
      lsbl.setAlpha(0.4f)

      findViewById(R.id.cardView1).asInstanceOf[ImageView].setAlpha(1f) //highlight the minideck preflop#1
      findViewById(R.id.cardView2).asInstanceOf[ImageView].setAlpha(1f) //highlight the minideck preflop#2

      //alpha the remaining
      findViewById(R.id.cardView3).asInstanceOf[ImageView].setAlpha(0.4f)
      findViewById(R.id.cardView4).asInstanceOf[ImageView].setAlpha(0.4f)
      findViewById(R.id.cardView5).asInstanceOf[ImageView].setAlpha(0.4f)
      findViewById(R.id.cardView6).asInstanceOf[ImageView].setAlpha(0.4f)
      findViewById(R.id.cardView7).asInstanceOf[ImageView].setAlpha(0.4f)

      findViewById(R.id.vListView).asInstanceOf[ListView].setAlpha(0.4f)  //villain range

      mFlipperView2.setAlpha(0.2f)

    }
  }) //listener


    //Listener 2
    wv2.addChangingListener(new OnWheelChangedListener {
      def onChanged(wheel: WheelView, oldValue: Int, newValue: Int) {

        //ObjectUtils.showToast("onChanged "+newValue,getBaseContext)
        if (!scrolling) {
        }
      }
    })
    wv2.addScrollingListener(new OnWheelScrollListener {
      def onScrollingStarted(wheel: WheelView) {
        scrolling = true
        //ObjectUtils.showToast("onScrollingStarted",getBaseContext)
      }

      def onScrollingFinished(wheel: WheelView) {
        scrolling = false
        //ObjectUtils.showToast("onScrollingFinished",getBaseContext)

        freezeOnRedback(1)

        //ObjectUtils.showToast("onScrollingFinished "+wheel.getCurrentItem,getBaseContext)


        //wv1.setEnabled(false) //lock the previous wheel
        updateWheelRotor(3,wheel.getCurrentItem)




        initPfChart

        //get pf strength (cards have been updated)
        buildPfStrengthBarGraph


        //all-in equity
        //TODO


        //tests here for various options

        /* why #2 but not #1?

        val hiStatLineH:TextView=findViewById(R.id.MiniStatusBar).asInstanceOf[TextView]
        hiStatLineH.setText("z")

        findViewById(R.id.MiniStatusBar).asInstanceOf[TextView].setText("z")

        */

      pfeqLine(gamePlayers)


        //sr.startListening(RecognizerIntent.getVoiceDetailsIntent(getApplicationContext()))

        //set alpha on flop
        wv3.setAlpha(1)
        wv4.setAlpha(1)
        wv5.setAlpha(1)

        //reset flop if coming back
        wv3.setCurrentItem(52)
        wv4.setCurrentItem(52)
        wv5.setCurrentItem(52)
        wv6.setCurrentItem(52)
        wv7.setCurrentItem(52)

        //and mini status
        //9-aug-14 reset the mini status
        statusCardsReset(2)

        //10-aug-14 add the card
        findViewById(R.id.cardView2).asInstanceOf[ImageView].setImageResource(ObjectUtils.refDeckImages(wv2.getCurrentItem))

        //10-aug-14 TODO on a repeat needs to write original cards to date
        //meaning we need to exec the write cards routine again, poss save to a tempMiniCardarray
        //only applicable if 2-4 card pos are selected as at 5/6/7 we rewrite on result
        findViewById(R.id.cardView1).asInstanceOf[ImageView].setImageResource(ObjectUtils.refDeckImages(wv1.getCurrentItem))



        //TODO 10-aug-14 and then highlight the flop (unset alpha)
        /*
        findViewById(R.id.cardView3).asInstanceOf[ImageView].setBackgroundDrawable(R.drawable.red_back)
        findViewById(R.id.cardView4).asInstanceOf[ImageView].setBackgroundDrawable(R.drawable.red_back)
        findViewById(R.id.cardView5).asInstanceOf[ImageView].setBackgroundDrawable(R.drawable.red_back)
        */

        val hiStatLineH=findViewById(R.id.MiniStatusBar).asInstanceOf[TextView]
        hiStatLineH.setText("Please enter flop cards...")

        //and any alpha
        lsl.setAlpha(0.4f)
        lsbl.setAlpha(0.4f)
        mFlipperView2.setAlpha(0.2f)

        lockForwardIfRedBackCurrent(2)

        findViewById(R.id.cardView3).asInstanceOf[ImageView].setAlpha(1f) //highlight the minideck flop#1
        findViewById(R.id.cardView4).asInstanceOf[ImageView].setAlpha(1f) //highlight the minideck flop#2
        findViewById(R.id.cardView5).asInstanceOf[ImageView].setAlpha(1f) //highlight the minideck flop#3

        findViewById(R.id.vListView).asInstanceOf[ListView].setAlpha(0.4f)  //villain range



      }
    }) //listener



    //Listener 3

    wv3.addChangingListener(new OnWheelChangedListener {
      def onChanged(wheel: WheelView, oldValue: Int, newValue: Int) {
        if (!scrolling) {
        }
      }
    })
    wv3.addScrollingListener(new OnWheelScrollListener {
      def onScrollingStarted(wheel: WheelView) {
        scrolling = true
        //ObjectUtils.showToast("onScrollingStarted",getBaseContext)
      }

      def onScrollingFinished(wheel: WheelView) {
        scrolling = false
        //ObjectUtils.showToast("onScrollingFinished",getBaseContext)

        freezeOnRedback(2)

        trashTheHandDown(5) //wipe the t/r
        trashTheHandDown(6)


        //mid-point for scroll
        //updateWheelCardMid(wv4)
        //updateWheelCardMid(wv5)
        //wv1.setEnabled(false) //lock both pf wheels             //23-Jan-14     //23-Jan-14 pf locked again //9-aug-14 unlocked for reset mandatory
        //wv2.setEnabled(false) //lock the previous wheel
        updateWheelRotor(4,wheel.getCurrentItem)

        statusCardsReset(3)

        //10-aug-14 add the card
        findViewById(R.id.cardView3).asInstanceOf[ImageView].setImageResource(ObjectUtils.refDeckImages(wv3.getCurrentItem))

        findViewById(R.id.cardView1).asInstanceOf[ImageView].setImageResource(ObjectUtils.refDeckImages(wv1.getCurrentItem))
        findViewById(R.id.cardView2).asInstanceOf[ImageView].setImageResource(ObjectUtils.refDeckImages(wv2.getCurrentItem))


        //09-aug-14
        //reset wheel

        wv4.setCurrentItem(52)
        wv5.setCurrentItem(52)
        wv6.setCurrentItem(52)
        wv7.setCurrentItem(52)

        if (VERSION=="Pro") {
          lsl.setAlpha(0.4f)
          lsbl.setAlpha(0.4f)
          mFlipperView2.setAlpha(0.2f)
        }
        else
        {
          lsl.setAlpha(0.15f)
          lsbl.setAlpha(0.15f)
          mFlipperView2.setAlpha(0.15f)
        }

        val hiStatLineH=findViewById(R.id.MiniStatusBar).asInstanceOf[TextView]
        hiStatLineH.setText("Please enter flop cards...")

        //20-jul-14 lock out pf as the status overwrites the flop status
        //findViewById(R.id.buttonHU).asInstanceOf[Button].setEnabled(false)
        //findViewById(R.id.button3M).asInstanceOf[Button].setEnabled(false)
        //findViewById(R.id.button6M).asInstanceOf[Button].setEnabled(false)
        //findViewById(R.id.button9M).asInstanceOf[Button].setEnabled(false)
        //findViewById(R.id.button10M).asInstanceOf[Button].setEnabled(false)

        lockForwardIfRedBackCurrent(3)

        //alpha the t/r
        findViewById(R.id.cardView6).asInstanceOf[ImageView].setAlpha(0.4f)
        findViewById(R.id.cardView7).asInstanceOf[ImageView].setAlpha(0.4f)

        findViewById(R.id.vListView).asInstanceOf[ListView].setAlpha(0.4f)  //villain range

      }
    }) //listener

    //Listener 4

    wv4.addChangingListener(new OnWheelChangedListener {
      def onChanged(wheel: WheelView, oldValue: Int, newValue: Int) {

        if (!scrolling) {
        }
      }
    })
    wv4.addScrollingListener(new OnWheelScrollListener {
      def onScrollingStarted(wheel: WheelView) {
        scrolling = true
        //ObjectUtils.showToast("onScrollingStarted",getBaseContext)
      }

      def onScrollingFinished(wheel: WheelView) {
        scrolling = false
        //ObjectUtils.showToast("onScrollingFinished",getBaseContext)
        //wv3.setEnabled(false) //lock the previous wheel

        freezeOnRedback(3)

        trashTheHandDown(5) //wipe the t/r
        trashTheHandDown(6)

        updateWheelRotor(5,wheel.getCurrentItem)

        statusCardsReset(4)

        //10-aug-14 add the card
        findViewById(R.id.cardView4).asInstanceOf[ImageView].setImageResource(ObjectUtils.refDeckImages(wv4.getCurrentItem))

        findViewById(R.id.cardView1).asInstanceOf[ImageView].setImageResource(ObjectUtils.refDeckImages(wv1.getCurrentItem))
        findViewById(R.id.cardView2).asInstanceOf[ImageView].setImageResource(ObjectUtils.refDeckImages(wv2.getCurrentItem))
        findViewById(R.id.cardView3).asInstanceOf[ImageView].setImageResource(ObjectUtils.refDeckImages(wv3.getCurrentItem))


        wv5.setCurrentItem(52)
        wv6.setCurrentItem(52)
        wv7.setCurrentItem(52)

        if (VERSION=="Pro") {
          lsl.setAlpha(0.4f)
          lsbl.setAlpha(0.4f)
          mFlipperView2.setAlpha(0.2f)
        }
        else
        {
          lsl.setAlpha(0.15f)
          lsbl.setAlpha(0.15f)
          mFlipperView2.setAlpha(0.15f)
        }



        lockForwardIfRedBackCurrent(4)

        //alpha the t/r
        findViewById(R.id.cardView6).asInstanceOf[ImageView].setAlpha(0.4f)
        findViewById(R.id.cardView7).asInstanceOf[ImageView].setAlpha(0.4f)

        //alpha flipper2
        mFlipperView2.setAlpha(0.4f)

        findViewById(R.id.vListView).asInstanceOf[ListView].setAlpha(0.4f)  //villain range

      }
    }) //listener

    //Listener 5

    wv5.addChangingListener(new OnWheelChangedListener {
      def onChanged(wheel: WheelView, oldValue: Int, newValue: Int) {

        if (!scrolling) {
        }
      }
    })
    wv5.addScrollingListener(new OnWheelScrollListener {
      def onScrollingStarted(wheel: WheelView) {
        scrolling = true
        //ObjectUtils.showToast("onScrollingStarted",getBaseContext)
      }

      def onScrollingFinished(wheel: WheelView) {
        scrolling = false
        //ObjectUtils.showToast("onScrollingFinished",getBaseContext)
        //wv4.setEnabled(false) //lock the previous wheel

        freezeOnRedback(4)

        trashTheHandDown(5) //wipe the t/r
        trashTheHandDown(6)

        updateWheelRotor(6,wheel.getCurrentItem)
        wv6.setAlpha(1)

        displayAlgRanking5()

        //unblock pf/f red backs
        civ1.setAlpha(1)
        civ2.setAlpha(1)
        civ3.setAlpha(1)
        civ4.setAlpha(1)
        civ5.setAlpha(1)


        //show stats IF pro

        if (VERSION=="Pro") {
          lsl.setAlpha(1f)
          lsbl.setAlpha(1f)
          mFlipperView2.setAlpha(1f)
        }
        else
        {
          lsl.setAlpha(0.15f)
          lsbl.setAlpha(0.15f)
          mFlipperView2.setAlpha(0.15f)
        }
        //unblock flipper


        mFlipperView.setEnabled(true)
        mFlipperView2.setEnabled(true)



        wv6.setCurrentItem(52)
        wv7.setCurrentItem(52)

        //reset alpha to clear
        findViewById(R.id.flop_list).asInstanceOf[TableRow].setAlpha(1f)

        lockForwardIfRedBackCurrent(5)

        findViewById(R.id.cardView6).asInstanceOf[ImageView].setAlpha(1f) //highlight the minideck turn
        findViewById(R.id.cardView7).asInstanceOf[ImageView].setAlpha(1f) //highlight the minideck river

        //highlight the postflop info
        findViewById(R.id.rchart).asInstanceOf[FrameLayout].setAlpha(1f) //hero/villain range bar graph
        findViewById(R.id.mypos).asInstanceOf[FrameLayout].setAlpha(1f) //equity monte carlo
        findViewById(R.id.vListView).asInstanceOf[ListView].setAlpha(1f)  //villain range
        //findViewById(R.id.status_view).asInstanceOf[LinearLayout].setAlpha(1f) //hero outs
        findViewById(R.id.vlistView2).asInstanceOf[ListView].setAlpha(1f) //hero outs


      }
    }) //listener

    //Listener 6

    wv6.addChangingListener(new OnWheelChangedListener {
      def onChanged(wheel: WheelView, oldValue: Int, newValue: Int) {

        if (!scrolling) {
        }
      }
    })
    wv6.addScrollingListener(new OnWheelScrollListener {
      def onScrollingStarted(wheel: WheelView) {
        scrolling = true
        //ObjectUtils.showToast("onScrollingStarted",getBaseContext)
      }

      def onScrollingFinished(wheel: WheelView) {
        scrolling = false
        //ObjectUtils.showToast("onScrollingFinished",getBaseContext)
    //    wv3.setEnabled(false) //lock the previous flop wheel
    //    wv4.setEnabled(false) //lock the previous flop wheel
    //    wv5.setEnabled(false) //lock the previous flop wheel

        trashTheHandDown(6) //wipe river 24-aug-14

        freezeOnRedback(5)

        updateWheelRotor(7,wheel.getCurrentItem)
        wv6.setAlpha(1)

        //set outs window normal as outs available here

        val vlv2 = findViewById(R.id.vlistView2).asInstanceOf[ListView]

        if (VERSION=="Pro") {
          vlv2.setAlpha(1)
          mFlipperView2.setAlpha(1)
        }
        else
        {
          lsl.setAlpha(0.15f)
          lsbl.setAlpha(0.15f)
          vlv2.setAlpha(0.15f)
          mFlipperView2.setAlpha(0.15f)

        }




        displayAlgRanking67(6)


        lockForwardIfRedBackCurrent(6)


      }
    }) //listener

    //Listener 7

    wv7.addChangingListener(new OnWheelChangedListener {
      def onChanged(wheel: WheelView, oldValue: Int, newValue: Int) {

        if (!scrolling) {
        }
      }
    })
    wv7.addScrollingListener(new OnWheelScrollListener {
      def onScrollingStarted(wheel: WheelView) {
        scrolling = true
        //ObjectUtils.showToast("onScrollingStarted",getBaseContext)
      }

      def onScrollingFinished(wheel: WheelView) {
        scrolling = false

        freezeOnRedback(6)

        //ObjectUtils.showToast("onScrollingFinished",getBaseContext)
        //wv6.setEnabled(false) //lock the previous wheel
        //update itself
        //so not usual case here
          ObjectUtils.GlobalWheelBuilder(8,wheel.getCurrentItem)
          //val tvStatLine=findViewById(R.id.CurrentItemList).asInstanceOf[TextView]
          //tvStatLine.setText(""+ObjectUtils.currentPlayerCardsArrayDesc.deep)


          //jul-22-14 lock the turn wheel (temp)
          //wv6.setEnabled(false)

          //set outs window shaded as no outs available post river!
          val vlv2 = findViewById(R.id.vlistView2).asInstanceOf[ListView]

        if (VERSION=="Pro") {
          vlv2.setAlpha(0.3f)
          mFlipperView2.setAlpha(0.3f)
        }
        else{
          vlv2.setAlpha(0.15f)
          mFlipperView2.setAlpha(0.15f)
        }

          displayAlgRanking67(7)

        lockForwardIfRedBackCurrent(7)

      }
    }) //listener



  } //oc


  //Please note Pre-Flop equity values are subjective and the results line shows the results from two independent algorithms that generally are in concordance but may vary slightly

  private def pfeqLine(gamePlayers:Int){

    if (gamePlayers == 3) {
      //3MAX
      if (ObjectUtils.currentPlayerCardsArrayDesc(1).length > 0) {
        val aa = ObjectUtils.allinNmax(cryma3.tablemaxordering,ObjectUtils.currentPlayerCardsArrayDesc)
        val ab = PokerAlgs.preFlopHandStrength(PokerAlgs.Alg_ThreeMax,ObjectUtils.currentPlayerCardsArrayDesc)
        findViewById(R.id.bigStatusLine).asInstanceOf[TextView].setText("3MAX Pre-Flop Equity Value "+aa+"%"+" ("+ab+")")
      }}
    else if (gamePlayers == 6) {
      //6MAX
      if (ObjectUtils.currentPlayerCardsArrayDesc(1).length > 0) {
        val aa = ObjectUtils.allinNmax(cryma6.tablemaxordering,ObjectUtils.currentPlayerCardsArrayDesc)
        val ab = PokerAlgs.preFlopHandStrength(PokerAlgs.Alg_SixMax,ObjectUtils.currentPlayerCardsArrayDesc)
        findViewById(R.id.bigStatusLine).asInstanceOf[TextView].setText("6MAX Pre-Flop Equity Value "+aa+"%"+" ("+ab+")")
      }}
    //
    else if (gamePlayers == 10) {
      //10MAX
      if (ObjectUtils.currentPlayerCardsArrayDesc(1).length > 0) {
        val aa = ObjectUtils.allinNmax(cryma10.tablemaxordering,ObjectUtils.currentPlayerCardsArrayDesc)
        val ab = PokerAlgs.preFlopHandStrength(PokerAlgs.Alg_TenMax,ObjectUtils.currentPlayerCardsArrayDesc)
        findViewById(R.id.bigStatusLine).asInstanceOf[TextView].setText("10MAX Pre-Flop Equity Value "+aa+"%"+" ("+ab+")")
      }}
    else {

      //default

      if (ObjectUtils.currentPlayerCardsArrayDesc(1).length > 0) {
        //TODO cryma
        //val aa = ObjectUtils.allinNmax(cryma10.tablemaxordering, ObjectUtils.currentPlayerCardsArrayDesc)
        val ab = PokerAlgs.preFlopHandStrength(PokerAlgs.Alg_Random,ObjectUtils.currentPlayerCardsArrayDesc)
        findViewById(R.id.bigStatusLine).asInstanceOf[TextView].setText("[Default] Against Random Pre-Flop Equity Value "+ab+"%")
      }

      else findViewById(R.id.bigStatusLine).asInstanceOf[TextView].setText("Enter Pre-Flop Cards")

    }//pfeqline



  }


  /**
   * Updates pf to midway point for easy scrolling
   */
  private def updateWheelCardMid(cards_wv: WheelView) {

    cards_wv.setCurrentItem(ObjectUtils.currentDeck.length/2) //set wheel to midway for fast enter

  }

  //module: Updates the wheel and the current cards held array (via a call) and then draws it here
  private def updateWheelRotor(pos:Int, wheelSelectedItem:Int) {

  var resID=0

    // getResources.getIdentifier(rsc, "id", getPackageName) not working
    pos match {
    //preflop2
    case 2 =>   resID = R.id.card_display_2
    //flop 3-4-5
    case 3 =>   resID = R.id.card_display_3
    case 4 =>   resID = R.id.card_display_4
    case 5 =>   resID = R.id.card_display_5
    //turn
    case 6 =>   resID = R.id.card_display_6
    //river
    case 7 =>   resID = R.id.card_display_7


    case 0 => {ObjectUtils.showToast("ERROR updateWheel resID is "+resID,getBaseContext) }
  } //match


    //ObjectUtils.showToast(pos+" DO updateWheel resID is "+resID,getBaseContext)

      //manage the global wheel
      ObjectUtils.GlobalWheelBuilder(pos,wheelSelectedItem)

      //update view
      val wvn: WheelView = findViewById(resID).asInstanceOf[WheelView]
      val ca = new CardAdapter(this, ObjectUtils.currentDeck,ObjectUtils.currDeckImages) //with new wheel sans previous selected value
      ca.setTextSize(25)

      //ca.notifyDataChangedEvent()
      wvn.setViewAdapter(ca)


      //needs only be set once, and then ignored until next wheel
      wvn.setCurrentItem(52)


      //TODO only enable the wheel if the previous wheel is not in our current hand (we selected a 'taken' red back)
      //if... then
      wvn.setAlpha(1)
      wvn.setEnabled(true)

    //val tvStatLine=findViewById(R.id.CurrentItemList).asInstanceOf[TextView]
    //tvStatLine.setText("Deck "+ObjectUtils.currentPlayerCardsArrayDesc.deep)


  }



  //core callbacks
  override def onStart() {
    ObjectUtils.debugLog(TAG,"act_ onStart")
    super.onStart()

    EasyTracker.getInstance(this).activityStart(this) //google analytics
  }

  override def onRestart() {
    ObjectUtils.debugLog(TAG,"act_ onRestart")
    super.onRestart()

    prefs = this.getSharedPreferences("com.droidroid.PM2", Context.MODE_PRIVATE)
    // do the reverse operation
    val gson:Gson  = new Gson()
    val user_json0 = prefs.getString("bun0", "")

    //Log.d(TAG,"sp restoring json currentPlayerCardsArrayDesc "+user_json0)
    ObjectUtils.currentPlayerCardsArrayDesc  = gson.fromJson(user_json0, classOf[Array[String]])

    val user_json1 = prefs.getString("bun1", "")
    //Log.d(TAG,"sp restoring json currDeckImages "+user_json1)
    ObjectUtils.currDeckImages  = gson.fromJson(user_json1, classOf[Array[Int]])

    val user_json2 = prefs.getString("bun2", "")
    //Log.d(TAG,"sp restoring json currentPlayerCardsArrayRsc "+user_json2)
    ObjectUtils.currentPlayerCardsArrayRsc  = gson.fromJson(user_json2, classOf[Array[Int]])

  }

  override def onResume() {
    ObjectUtils.debugLog(TAG,"act_ onResume")
    super.onResume()
    //register rx
    //c-s-r(register here but we could also manifest it) - onStart/onStop p-s-d always matched
    // Register mMessageReceiver to receive messages.

    EasyTracker.getInstance(this).activityStop(this)

    LocalBroadcastManager.getInstance(this).registerReceiver(equityBroadcastReceiver, new IntentFilter(EquityBroadcastServiceWrapper.BROADCAST_ACTION))

  }

  override def onPause() {
    ObjectUtils.debugLog(TAG,"act_ onPause")
    super.onPause()

    //this.stopService(service_intent)

    try {
      LocalBroadcastManager.getInstance(this).unregisterReceiver(equityBroadcastReceiver)
    } catch {
      case e: Exception => {
        e.printStackTrace
        //Log.wtf(TAG, "leaky rx bug")
        //ObjectUtils.showToast("err")
      }
    }


    //save objects
    // convert User object user to JSON format
    val gson:Gson  = new Gson()

    // store in SharedPreferences
    prefs = this.getSharedPreferences("com.droidroid.PM2", Context.MODE_PRIVATE)
    val user_json_bun0 = gson.toJson(ObjectUtils.currentPlayerCardsArrayDesc)
    //Log.d(TAG,"sp putting json currentPlayerCardsArrayDesc "+user_json_bun0)
    prefs.edit().putString("bun0", user_json_bun0).apply()

    val user_json_bun1 = gson.toJson(ObjectUtils.currDeckImages)
    //Log.d(TAG,"sp putting json currDeckImages "+user_json_bun1)
    prefs.edit().putString("bun1", user_json_bun1).apply()

    val user_json_bun2 = gson.toJson(ObjectUtils.currentPlayerCardsArrayRsc)
    //Log.d(TAG,"sp putting json currentPlayerCardsArrayRsc "+user_json_bun2)
    prefs.edit().putString("bun2", user_json_bun2).apply()

    prefs.edit().commit()


  }

  override def onStop() {
    ObjectUtils.debugLog(TAG,"act_ onStop")
    super.onStop()
    ObjectUtils.resetCardData()

  }

  override def onDestroy() {
    ObjectUtils.debugLog(TAG,"act_ onDestroy")
    prefs = this.getSharedPreferences("com.droidroid.PM2", Context.MODE_PRIVATE)
    //don't need anymore
    prefs.edit().putString("bun0", "").apply()
    prefs.edit().putString("bun1", "").apply()
    prefs.edit().putString("bun2", "").apply()

    try {
    // Unbind from the service
    unbindService(mConnection) }
    catch {
      case e: Exception => { //17-aug-14 workaround
        //java.lang.RuntimeException: Unable to destroy activity {com.droidroid.PM2/com.droidroid.PM2.PrimaryActivity}: java.lang.IllegalArgumentException: Service not registered: com.droidroid.PM2.PrimaryActivity$$anon$24@414dc4d8
        //e.printStackTrace
      }
    }



    super.onDestroy()

  }



//
  /*


  override def onSaveInstanceState(state:Bundle) {
    super.onSaveInstanceState(state)
    Log.v(TAG, "onSaveInstanceState")
    state.putSerializable("oj", ObjectUtils.currentPlayerCardsArrayRsc)
  }


  override def onRestoreInstanceState(savedInstanceState:Bundle) {
    super.onRestoreInstanceState(savedInstanceState)
    Log.v(TAG, "onRestoreInstanceState")
    ObjectUtils.currentPlayerCardsArrayRsc = savedInstanceState.getSerializable("oj").asInstanceOf
  }
*/

  //

  def onClickLogoHelp(v:View): Unit = {
        val uintent:Intent  = new Intent()
        uintent.setAction(Intent.ACTION_VIEW)
        uintent.addCategory(Intent.CATEGORY_BROWSABLE)
        uintent.setData(Uri.parse("https://play.google.com/store/apps/details?id=com.droidroid.PM2"))
        startActivity(uintent)
  }

  //


  def onClickFlipChart(v:View): Unit = {

    Toast.makeText(getApplicationContext(), "Graph View", Toast.LENGTH_SHORT).show()


    // Set the content view to 0% opacity but visible, so that it is visible
    // (but fully transparent) during the animation.
    //mFlipperView.setAlpha(0f)
    mFlipperView.setVisibility(View.VISIBLE)

    // Animate the content view to 100% opacity, and clear any animation
    // listener set on the view.
    //mFlipperView.animate()
    //  .alpha(1f)
    //  .setDuration(mShortAnimationDuration)
    //  .setListener(null)

    // Animate the loading view to 0% opacity. After the animation ends,
    // set its visibility to GONE as an optimization step (it won't
    // participate in layout passes, etc.)
    //mFlipperView2.animate()
    //  .alpha(0f)
    //  .setDuration(mShortAnimationDuration)
    //  .setListener(new AnimatorListenerAdapter() {
    //  override def onAnimationEnd(animation:Animator) {
        mFlipperView2.setVisibility(View.GONE)
    //  }
    //})

  }

  //called from onClick for the LL
  def onClickFlipChart2(v:View): Unit = {

    Toast.makeText(getApplicationContext(), "Odds View", Toast.LENGTH_SHORT).show()


    // Set the content view to 0% opacity but visible, so that it is visible
    // (but fully transparent) during the animation.
    //mFlipperView2.setAlpha(0f)
    mFlipperView2.setVisibility(View.VISIBLE)

    // Animate the content view to 100% opacity, and clear any animation
    // listener set on the view.
    //mFlipperView2.animate()
    //  .alpha(1f)
    //  .setDuration(mShortAnimationDuration)
    //  .setListener(null)

    // Animate the loading view to 0% opacity. After the animation ends,
    // set its visibility to GONE as an optimization step (it won't
    // participate in layout passes, etc.)
    //mFlipperView.animate()
    //  .alpha(0f)
    //  .setDuration(mShortAnimationDuration)
    //  .setListener(new AnimatorListenerAdapter() {
    //  override def onAnimationEnd(animation:Animator) {
        mFlipperView.setVisibility(View.GONE)
    //  }
    //})

  }





  def setUpQAV(){
    //uses library http://www.londatiga.net/it/how-to-create-quickaction-dialog-in-android/


    //val ID_INFO: Int = 4
    val HIDE_FI: Int = 1
    val HIDE_EQ: Int = 2
    val HIDE_SE: Int = 3
    prefs = this.getSharedPreferences("com.droidroid.PM2", Context.MODE_PRIVATE)



    var show_weak:Int = prefs.getInt("filter-none-hero",0)
    if (show_weak==0) { //initially
      prefs.edit().putInt("filter-none-hero", 1).apply()
      show_weak = 1
    }


    var show_less_rsc:Int=0
    if (show_weak == 1) show_less_rsc = R.drawable.menu_ok else show_less_rsc = R.drawable.menu_cancel

    //var show_equity:Int = prefs.getInt("highlight-weak-hands",0)
    //if (show_equity==0) { //initially
    //  prefs.edit().putInt("highlight-weak-hands", 1).apply()
    //  show_equity = 1
    //}
    //var show_less_rsc2:Int=0
    //if (show_equity == 1) show_less_rsc2 = R.drawable.menu_ok else show_less_rsc2 = R.drawable.menu_cancel

    var show_strict_equity:Int = prefs.getInt("strict-equity",0)
    if (show_strict_equity==0) { //initially
      prefs.edit().putInt("strict-equity", 1).apply()
      show_strict_equity = 1
    }
    var show_less_rsc3:Int=0
    if (show_strict_equity == 1) show_less_rsc3 = R.drawable.menu_ok else show_less_rsc3 = R.drawable.menu_cancel





    quickActionV = new QuickAction(this, QuickAction.VERTICAL)

    //val infoItem: ActionItem = new ActionItem(ID_INFO, ""+Math.random(), getResources.getDrawable(R.drawable.menu_info))

    val fiItem:ActionItem  		= new ActionItem(HIDE_FI, "Show all Hero Outs in Table", getResources().getDrawable(show_less_rsc))
    //val eqItem:ActionItem  		= new ActionItem(HIDE_EQ, "Highlight Weak pre-flop Villains", getResources().getDrawable(show_less_rsc2))
    val seqItem:ActionItem  		= new ActionItem(HIDE_SE, "Standard Equity Calculation", getResources().getDrawable(show_less_rsc3))

    //val maniacVillains:ActionItem  		= new ActionItem(HIDE_OK, "maniac", getResources().getDrawable(show_less_rsc))
    //val looseVillains:ActionItem  		= new ActionItem(HIDE_OK, "loose", getResources().getDrawable(show_less_rsc))
    //val averageVillains:ActionItem  		= new ActionItem(HIDE_OK, "average", getResources().getDrawable(show_less_rsc))
    //val tightVillain:ActionItem  		= new ActionItem(HIDE_OK, "tight", getResources().getDrawable(show_less_rsc))
    //val supertightVillain:ActionItem  		= new ActionItem(HIDE_OK, "supertight", getResources().getDrawable(show_less_rsc))

    //val hIcon: ActionItem = new ActionItem(ID_INFO, ""+Math.random(), getResources.getDrawable(R.drawable.hero_icon))
    //val vIcon1:ActionItem  		= new ActionItem(HIDE_OK, "boo boo", getResources().getDrawable(R.drawable.villain_icon))
   // val vIcon2:ActionItem  		= new ActionItem(HIDE_OK, "boo boo2", getResources().getDrawable(R.drawable.villain2_icon))

    //use setSticky(true) to disable QuickAction dialog being dismissed after an item is clicked
    //zzz.setSticky(true)

    //quickActionV.addActionItem(infoItem)
    quickActionV.addActionItem(fiItem)
    //quickActionV.addActionItem(eqItem)
    quickActionV.addActionItem(seqItem)

    //quickActionV.addActionItem(maniacVillains)
    //quickActionV.addActionItem(looseVillains)
    //quickActionV.addActionItem(averageVillains)
    //quickActionV.addActionItem(tightVillain)
    //quickActionV.addActionItem(supertightVillain)

    //Set listener for action item clicked
    quickActionV.setOnActionItemClickListener(new QuickAction.OnActionItemClickListener() {

      override def onItemClick(source:QuickAction , pos:Int, actionId:Int) {

        val actionItem:ActionItem = quickActionV.getActionItem(pos)

        //here we can filter which action item was clicked with pos or actionId parameter

        //if (actionId == HIDE_EQ) {
        //  //switch
        //  if (show_equity ==2 )  {
        //    prefs.edit().putInt("highlight-weak-hands",1).apply()
        //    Toast.makeText(getApplicationContext(), "Highlighting weak hands", Toast.LENGTH_SHORT).show()
        //  }
        //  else
        //  if (show_equity ==1 )  {
        //    prefs.edit().putInt("highlight-weak-hands",2).apply()
        //    Toast.makeText(getApplicationContext(), "Hiding weak hands", Toast.LENGTH_SHORT).show()
        //  }
        //}  //HIDE_EQ





        if (actionId == HIDE_FI) {
          //val ll = findViewById(R.id.status_view).asInstanceOf[LinearLayout]
          //val lv:ListView = findViewById(R.id.vlistView2).asInstanceOf[ListView]
          //switch
          if (show_weak ==2 )  {
            prefs.edit().putInt("filter-none-hero",1).apply()
            Toast.makeText(getApplicationContext(), "Outs: Show all hands", Toast.LENGTH_SHORT).show()
          }
          else
          if (show_weak ==1 )  {
            prefs.edit().putInt("filter-none-hero",2).apply()
            Toast.makeText(getApplicationContext(), "Outs: Showing only Hero", Toast.LENGTH_SHORT).show()
          }
        }  //HIDE_FI

        if (actionId == HIDE_SE) {
          //val ll = findViewById(R.id.status_view).asInstanceOf[LinearLayout]
          //val lv:ListView = findViewById(R.id.vlistView2).asInstanceOf[ListView]
          //switch
          if (show_strict_equity ==2 )  {
            prefs.edit().putInt("strict-equity",1).apply()
            Toast.makeText(getApplicationContext(), "Strict Equity Calcs", Toast.LENGTH_SHORT).show()
          }
          else
          if (show_strict_equity ==1 )  {
            prefs.edit().putInt("strict-equity",2).apply()
            Toast.makeText(getApplicationContext(), "Loose Equity Calcs", Toast.LENGTH_SHORT).show()
          }
        }  //HIDE_FI




      }
    })


    quickActionV.setOnDismissListener(new QuickAction.OnDismissListener() {
      @Override
      override def onDismiss() {
        //Toast.makeText(getApplicationContext(), "Dismissed", Toast.LENGTH_SHORT).show()
      }
    })



  }

  def setUpQAH(position:Int) {
    //uses library http://www.londatiga.net/it/how-to-create-quickaction-dialog-in-android/


    val ID_INFO: Int = 4

    quickActionH = new QuickAction(this, QuickAction.HORIZONTAL)

    prefs = this.getSharedPreferences("com.droidroid.PM2", Context.MODE_PRIVATE)
    val hnds:String = prefs.getString("sh" + (position+1),"-sherror-")
    val mpoz:Int = prefs.getInt("mpos",0) //actual hero position in lv
    var stats:String=""
    //26-7-14 TODO this is because we don't have full pf information only the returned best 5
    //would need to modify outs5 ndk func to return the full pf associated with the hand! (villain6/7)

    var infoItem:ActionItem=null

    //16-8-14, 20-8-14
    if (mpoz==position+1) { //h
      stats="Hero (Player)"
      infoItem = new ActionItem(ID_INFO, stats, getResources.getDrawable(R.drawable.hero_icon))
    }
    else if (mpoz > (position+1)) { //v-hi
      stats="preflop "+hnds
      infoItem = new ActionItem(ID_INFO, stats, getResources.getDrawable(R.drawable.villain_icon))
    }
    else { //v-lo
      stats="preflop "+hnds
      infoItem = new ActionItem(ID_INFO, stats, getResources.getDrawable(R.drawable.villain2_icon))
    }



    //use setSticky(true) to disable QuickAction dialog being dismissed after an item is clicked
    //zzz.setSticky(true)

    quickActionH.addActionItem(infoItem)

    //Set listener for action item clicked
    quickActionH.setOnActionItemClickListener(new QuickAction.OnActionItemClickListener() {

      override def onItemClick(source: QuickAction, pos: Int, actionId: Int) {

        val actionItem: ActionItem = quickActionH.getActionItem(pos)

        //here we can filter which action item was clicked with pos or actionId parameter

        if (actionId == ID_INFO) {
          //Toast.makeText(getApplicationContext(), "I have no info this time", Toast.LENGTH_SHORT).show()
        }
      }
    })


    quickActionH.setOnDismissListener(new QuickAction.OnDismissListener() {
      @Override
      override def onDismiss() {
        //Toast.makeText(getApplicationContext(), "Dismissed", Toast.LENGTH_SHORT).show()
      }
    })
  }





  val bc:Int = Color.GRAY
  val tc:Int= Color.BLACK

  var pB:Int=0
  //Module: Handlers for the non-wheel UI
  def onClickButtonOPTIONS(v:View): Unit = {
    ObjectUtils.showToast("Heads Up",getBaseContext)
    //val mButton:Button=(Button)findViewById(R.id.button1)
    //val mButton:Button=findViewById(R.id.buttonSB).asInstanceOf[Button]
    //mButton.setTextColor(Color.parseColor("#FFFF00")) // custom color

    //TODO 1-8-14 move to QAV options
    //ObjectUtils.setBoardPosition(1)
    //gamePlayers=1
    //pfeqLine(gamePlayers)

    setUpQAV()
    //item handled in the quickAction overridden handler in quickActionV
    quickActionV.show(v)

  }


  def onClickButtonCA(v:View): Unit = {
    ObjectUtils.showToast("Clear All",getBaseContext)
    findViewById(R.id.buttonCA).asInstanceOf[Button].setTextColor(Color.parseColor("#FFFFFF")) // custom color
    findViewById(R.id.buttonCA).asInstanceOf[Button].setBackgroundColor(Color.parseColor("#c05a17")) // custom color

    DoRestart.doRestart(this)

    //pBa=R.id.buttonCA
    //ObjectUtils.resetCardData()

    //reset application
    //http://stackoverflow.com/questions/6609414/howto-programatically-restart-android-app
    /*
    val ctx = this
    val mStartActivityIntent  = new Intent(PrimaryActivity.this.getBaseContext(), classOf[PrimaryActivity])


    val mPendingIntentId:Int = 123456
    val mPendingIntent:PendingIntent  = PendingIntent.getActivity(ctx, mPendingIntentId,    mStartActivityIntent, PendingIntent.FLAG_CANCEL_CURRENT)
    val mgr:AlarmManager  = ctx.getSystemService(Context.ALARM_SERVICE).asInstanceOf[AlarmManager]
    mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent)
    System.exit(0)
    */
  }



 //common to button handlers
  def clearButtonColorSelectors(mId:Int): Unit = { //as per xml button inflation colors
    findViewById(mId).asInstanceOf[Button].setTextColor(Color.parseColor("#ffffff"))
    findViewById(mId).asInstanceOf[Button].setBackgroundResource(drawable.dialog_holo_dark_frame)  //TODO needs to match original color
  }










  //achartengine core code

  private def getPfMyBarDataset: XYMultipleSeriesDataset = {
    PfbuildInitialRange(75)
  }



  def getPfMyBarRenderer: XYMultipleSeriesRenderer = {
    val renderer: XYMultipleSeriesRenderer = new XYMultipleSeriesRenderer

    renderer.setAxisTitleTextSize(15)
    renderer.setChartTitleTextSize(15)
    renderer.setLabelsTextSize(17)
    renderer.setLegendTextSize(17)
    renderer.setMargins(Array[Int](30, 40, 15, 25))  //tlbr

    //java.lang.ClassCastException: org.achartengine.renderer.SimpleSeriesRenderer cannot be cast to org.achartengine.renderer.XYSeriesRenderer
    //1.1 to 1.2 changed from simpleseries to xyseries

    val r: XYSeriesRenderer = new XYSeriesRenderer
    r.setColor(Color.RED)
    renderer.addSeriesRenderer(r)
    val r2: XYSeriesRenderer = new XYSeriesRenderer
    r2.setColor(Color.GREEN)
    renderer.addSeriesRenderer(r2)
    val r3: XYSeriesRenderer = new XYSeriesRenderer
    r3.setColor(Color.YELLOW)

    renderer.addSeriesRenderer(r3)
    renderer
  }

  private def myPfChartSettings(renderer: XYMultipleSeriesRenderer) {

    renderer.setChartTitle("Pre-Flop Percentile Rank")

    //renderer.setDisplayChartValues(true)

    //renderer.setDisplayChartValues(true) //distracting and too small, we only need to see our position

    //horiz
    renderer.setXLabelsAlign(Align.CENTER)
    renderer.setXAxisMin(0)
    renderer.setXAxisMax(5)
    renderer.setXLabels(0)
    //renderer.setXLabelsAngle(35)

    //vert
    renderer.setYLabelsAlign(Align.RIGHT)
    renderer.setYAxisMin(1)
    renderer.setYAxisMax(100)
    renderer.setYLabels(5)
    renderer.setYLabelsAngle(45)


    if (device_width<=1280) {

      renderer.setBarSpacing(-0.5) //in bar charts
      renderer.setBarWidth(15f) //width of each bar in pixels

    } else {

    renderer.setBarSpacing(-0.5) //in bar charts
    renderer.setBarWidth(30f) //width of each bar in pixels
    }
                      //tlbr
    //val mg:Array[Int]=Array(15, 15, 5, 0)
    //renderer.setMargins(mg)

    renderer.addXTextLabel(1, "w.vs.r")
    renderer.addXTextLabel(2, "6M")
    renderer.addXTextLabel(3, "10M")
    renderer.addXTextLabel(4, "chen(n)")

    renderer.setShowLabels(true)


    renderer.setApplyBackgroundColor(true)
    renderer.setGridColor(Color.argb(0x00, 0x01, 0x01, 0x01))
    renderer.setMarginsColor(Color.argb(0x00, 0x01, 0x01, 0x01))
    renderer.setBackgroundColor(Color.argb(0x00, 0x01, 0x01, 0x01))

    //renderer.setXTitle("[Alg:Equity vs Random]")
    renderer.setYTitle("strength(%)")
    renderer.setShowGrid(true)
    renderer.setGridColor(Color.GRAY)

    //6-aug-14
    renderer.setPanEnabled(false, false)
    renderer.setClickEnabled(true)
    //renderer.setSelectableBuffer(10)


  }

  private def PfbuildInitialRange(mp:Int): XYMultipleSeriesDataset  = {

    val dataset: XYMultipleSeriesDataset = new XYMultipleSeriesDataset

    val legendTitles: util.ArrayList[String] = new util.ArrayList[String]

    var heroPosition_1=0
    var heroPosition_2=0
    var heroPosition_3=0
    var heroPosition_4=0


    //we can only build if we have two values (else block as red = 0%)
    var pfl=0
    for (pfc <- ObjectUtils.currentPlayerCardsArrayDesc) {
      if (pfc.length>0) pfl+=1
    }

    if (pfl>1)
                      {
                      heroPosition_1 = PokerAlgs.preFlopHandStrength(PokerAlgs.Alg_Random,ObjectUtils.currentPlayerCardsArrayDesc).toInt
                      heroPosition_2 = PokerAlgs.preFlopHandStrength(PokerAlgs.Alg_SixMax,ObjectUtils.currentPlayerCardsArrayDesc).toInt
                      heroPosition_3 = PokerAlgs.preFlopHandStrength(PokerAlgs.Alg_TenMax,ObjectUtils.currentPlayerCardsArrayDesc).toInt
                      heroPosition_4 = (PokerAlgs.chenPfRank(ObjectUtils.currentPlayerCardsArrayDesc)*100/20)//normalized (x5)
                      }

    //26-Jul-14 test
    val chen=PokerAlgs.chenPfRank(ObjectUtils.currentPlayerCardsArrayDesc)
    ObjectUtils.showToast("CHEN "+chen,getBaseContext)

    //(RED) completes to 100 villain/hero
    if (device_width<=1280) legendTitles.add("Opp-Hi") else legendTitles.add("Opponent-H")
    val series1: CategorySeries = new CategorySeries(legendTitles.get(0))
    series1.add(100)
    series1.add(100)
    series1.add(100)
    series1.add(0)  //chen n/a
    dataset.addSeries(series1.toXYSeries)

    //(GREEN) hero
    legendTitles.add("Player")
    val series2: CategorySeries = new CategorySeries(legendTitles.get(1))
    series2.add(heroPosition_1 + 2)
    series2.add(heroPosition_2 + 2)
    series2.add(heroPosition_3 + 2)
    series2.add(heroPosition_4 + 2)
    dataset.addSeries(series2.toXYSeries)

    //(YELLOW) Villain
    if (device_width<=1280) legendTitles.add("Opp-Lo") else legendTitles.add("Opponent-L")
    val series3: CategorySeries = new CategorySeries(legendTitles.get(2))
    series3.add(heroPosition_1-2)
    series3.add(heroPosition_2-2)
    series3.add(heroPosition_3-2)
    series3.add(0)        //chen n/a
    dataset.addSeries(series3.toXYSeries)

    dataset

  }

  private def buildPfStrengthBarGraph {

    val cpc = ObjectUtils.currentPlayerCardsArrayDesc
    val pfStrength = PokerAlgs.preFlopHandStrength(PokerAlgs.Alg_Random,cpc)
    ObjectUtils.debugLog(TAG," Setting pf graph to : "+pfStrength+"%")



    //TODO 9-aug-14 write out preflop cards

    //achartengine core support
    val renderer: XYMultipleSeriesRenderer = getPfMyBarRenderer
    myPfChartSettings(renderer)

    val buildPfr:XYMultipleSeriesDataset  = PfbuildInitialRange(pfStrength.toInt)

    val mChartView = ChartFactory.getBarChartView(getApplicationContext(), buildPfr, renderer,Type.STACKED)

    //06-aug-14
    mChartView.setOnClickListener(new View.OnClickListener() {
      def onClick(v:View) {
        // handle the click event on the chart
        val aseriesSelection:SeriesSelection  = mChartView.getCurrentSeriesAndPoint()
        if (aseriesSelection == null) {
        //  Toast.makeText(XYChartBuilder.this, "No chart element", Toast.LENGTH_SHORT).show()
        ObjectUtils.showToast("clicked", getBaseContext)
        } else {
        //display information of the clicked point
        ObjectUtils.showToast("Chart element in series index " + aseriesSelection.getSeriesIndex()+ " data point index " + aseriesSelection.getPointIndex() + " was clicked"+" closest point value X=" + aseriesSelection.getXValue() + ", Y="+ aseriesSelection.getValue() ,getBaseContext)
        }
      }
    })


    val layout = findViewById(R.id.chart).asInstanceOf[LinearLayout]

    layout.setBackgroundResource(R.drawable.customborders_redv2)

    //21-7-14
    if (cpc(0) == cpc(1)) { //identical card
      //Log.d(TAG,"ident pf cards!")
      //freezeViews
    } else {

      layout.removeAllViews()
      layout.addView(mChartView)

      mChartView.repaint()
    }



    //tvStatLine.setText(pfActionLine)

    //val lwStatLine=findViewById(R.id.chenpf).asInstanceOf[TextView]
    //lwStatLine.setText("Chen Formula: "+PokerAlgs.chenPfRank(ObjectUtils.currentPlayerCardsArrayDesc)+"/20")

    //val lwStatLine2=findViewById(R.id.algpf).asInstanceOf[TextView]
    //lwStatLine2.setText("Weighted vs Random Strength: "+PokerAlgs.preFlopHandStrength(PokerAlgs.Alg_Random,ObjectUtils.currentPlayerCardsArrayDesc)+"%")
  }

  //Module: Flop Ranking for Hero (+Villain)
  def displayAlgRanking5(){

    //Module:Sub Hero5 best
    val acr = PokerAlgs.htable(ObjectUtils.currentPlayerCardsArrayDesc)
    //ObjectUtils.debugLog(TAG,"ndk cpcad="+(ObjectUtils.currentPlayerCardsArrayDesc) )
    ObjectUtils.debugLog(TAG,"ndk r5="+acr(0)+"/"+acr(1)+"/"+acr(2)+"/"+acr(3)+"/"+acr(4)+"/"+acr(5)+"/"+acr(6))

    //13-aug-14 we start here as we don't need beforehand as we don't have the data

    //service_intent.putExtra("hero",acr)
    //this.startService(service_intent)
    mService.be(acr,prefs.getInt("strict-equity",1))

      ObjectUtils.debugLog(TAG,"hero5 flop cardx="+acr(0)+"/"+acr(1)+"/"+acr(2)+"/"+acr(3)+"/"+acr(4))


      val racr:Array[Int] = coreEngine("hero5","",acr(0),acr(1),acr(2),acr(3),acr(4),0,0)
      var hhighest:String="error"
      val cr = racr(0)
      var hcr=0
      var hero_high=0


    try {
      if (cr > 6185)  { hhighest="10. High Card"; hcr=10; hero_high=cr  }    // 1277 high card
      else if (cr > 3325)  {hhighest="9. One Pair"; hcr=9; hero_high=cr }       // 2860 one pair
      else if (cr > 2467)  {hhighest="8. Two Pair"; hcr=8; hero_high=cr }       //  858 two pair
      else if (cr > 1609) {hhighest="7. Three of a Kind"; hcr=7; hero_high=cr }//  858 three-kind
      else if (cr > 1599)  {hhighest="6. Straight"; hcr=6; hero_high=cr }       //   10 straights
      else if (cr > 322)  {hhighest="5. Flush"; hcr=5; hero_high=cr }           // 1277 flushes
      else if (cr > 166)  {hhighest="4. Full House 3+2"; hcr=4; hero_high=cr }      //  156 full house
      else if (cr > 10)   {hhighest="3. Four of a Kind"; hcr=3; hero_high=cr }   //  156 four-kind
      else if (cr>1)   {hhighest="2. Straight Flush"; hcr=2; hero_high=cr }      //   10 straight-flushes
      else if (cr== 1)  {hhighest="1. Royal Flush"; hcr=1; hero_high=cr }
      else if (cr ==0)  hhighest="H-LO-ER Royal Flush?"   //TODO not sure if this error ever raised
    }
    catch {
      case e: Exception => {
        //Log.wtf(TAG,""+e.printStackTrace)
        Toast.makeText(this, "cr err", Toast.LENGTH_LONG).show()
      }
    }

    var opth,optv:String=""

    //fix 22-7-14
    var tcr=cr
    if (cr==0) tcr=1

    val hCombo:String = cry.htableTable_2(tcr-1)
    val hCombob:String = cry.htableTable_3(tcr-1)

    //display it
    val hiStatLineH=findViewById(R.id.MiniStatusBar).asInstanceOf[TextView]
    hiStatLineH.setText("Hero Flop: #"+hhighest+" ("+hCombo+")"+" "+hCombob)

    findViewById(R.id.bigStatusLine).asInstanceOf[TextView].setText("Hero Flop: #"+hhighest+" ("+hCombo+")"+" "+hCombob)
    this.findViewById(R.id.mypos).asInstanceOf[FrameLayout].setAlpha(1f)





    //Module:Sub Villain5 best
    ObjectUtils.debugLog(TAG,"villain5 flop cardx="+acr(0)+"/"+acr(1)+"/"+acr(2)+"/"+acr(3)+"/"+acr(4))
    //do villain best 5


    /*
    08-13 00:23:42.407  21623-21623/com.droidroid.PM2 E/AndroidRuntime﹕ FATAL EXCEPTION: main
    Process: com.droidroid.PM2, PID: 21623
    java.lang.ArrayIndexOutOfBoundsException: length=7462; index=1900171776
    raised down in VRs.... where it was picked up.. check there for debug code
    NDK output looked ok
    */
    //also
    //TODO
    /*
    08-15 22:29:03.030  20422-20422/com.droidroid.PM2 E/AndroidRuntime﹕ FATAL EXCEPTION: main
    java.lang.ArrayIndexOutOfBoundsException: length=7462; index=26140071
    */

    /* problem here with an entry way outside of the villain range placed into racr5 */

    /*
        08-15 22:29:02.585  20422-20422/com.droidroid.PM2 D/PrimaryActivity_﹕ 0 direct from ndk racr5 array unsorted is 26140072
        to
        08-15 22:29:02.593  20422-20422/com.droidroid.PM2 D/PrimaryActivity_﹕ 50 direct from ndk racr5 array unsorted is 26140072
     */

    val racr5:Array[Int] = coreEngine("villain5","",acr(0),acr(1),acr(2),acr(3),acr(4),0,0)

    /*15-aug-14 code ndk side... temp fixed this see approx line 654 in cEngine.c*/



    ObjectUtils.debugLog(TAG,"done villain5 ndk")


    //14-aug-14 debugging
    var ixa:Int=0
    for (rd <- racr5) {
      //Log.d(TAG,""+ixa+" direct from ndk racr5 array unsorted is "+rd)

      //if zero is returned then it is a rf? add 1 to it to make it stable TODO fix 14-aug-14
      //if (rd==0) Log.d(TAG,"racr5 zero value found at "+ixa)
      ixa+=1
    }

    //14-aug-14 kludge remove all zeros - this will affect villain range! but also does the sorting
    //val racr5_list = racr5.toList
    //racr5_list.reverse.dropWhile(_ == 0).reverse
    //racr5 = racr5.toArray

    scala.util.Sorting.quickSort(racr5)  //sort it results ascending 1..6185 (higher hands first)



    //14-8-14 last member of array corrupted/illegal here (after sort)
    //TODO replace kludge fix - THIS MAY CAUSE ISSUES WITH VILLAIN RANGE INACCURACIES!
    //val rl = racr5.size
    //if (racr5(rl-1) > 7461) {
    //  racr5(rl-2)=racr5(rl-1)
    //  Log.wtf(TAG, "racr5 issue to be fixed from kludge..")
    //}




    doVillainRange5(racr5,hero_high) //write villain range

    var vhighest:String="error"

    //log all possibles and place in an array 10 counter - for use as a hand range for pre-flop
    var la:Int=0
    val hc:Array[Int] = Array.fill[Int](racr5.length)(0)

    try {
      for (ra <- racr5) {
        ObjectUtils.debugLog(TAG, "" + la + " best is " + ra)

        ra match {

          case ra if (ra > 6185) => hc(10) += 1 // 1277 high card
          case ra if (ra > 3325) => hc(9) += 1 // 2860 one pair
          case ra if (ra > 2467) => hc(8) += 1 //  858 two pair
          case ra if (ra > 1609) => hc(7) += 1 //  858 three-kind
          case ra if (ra > 1599) => hc(6) += 1 //   10 straights
          case ra if (ra > 322) => hc(5) += 1 // 1277 flushes
          case ra if (ra > 166) => hc(4) += 1 //  156 full house
          case ra if (ra > 10) => hc(3) += 1 //  156 four-kind
          case ra if (ra > 1) => hc(2) += 1 //   10 straight-flushes
          case ra if (ra == 1) => hc(1) += 1 //RF

          case ra if (ra < 1) => {
            //Log.wtf(TAG, "racr5 error value is " + ra)
          } //error so ignore currently

        } //ra
        la += 1

      } //range loop
    }
    catch {
      case e: Exception => {
        //Log.wtf(TAG, "" + e.printStackTrace)
        Toast.makeText(this, "ra err", Toast.LENGTH_LONG).show()
      }
    }


    //for (vidx <- 1 to 10) ObjectUtils.debugLog(TAG,vidx+" available hands are "+hc(vidx))


    displayHandRange(hc, hcr)  //pass Villain range and hero actual


    val cr2 = racr5(0)

    vhighest=ObjectUtils.returnHand(cr2)

    //14-aug-14 TODO kludge fix
    if (cr2<=0) {
      //Log.wtf(TAG,"cr2 fail with "+cr2)
      Toast.makeText(this, "cr2 err", Toast.LENGTH_LONG).show()
    }


    val vCombo:String = cry.htableTable_2(cr2-1)  //TODO 22-02-14 -1 added,necessary?
    //val vCombob:String = cry.htableTable_3(cr2)

    //val hiStatLineV=findViewById(R.id.hiflopvillain).asInstanceOf[TextView]
    //hiStatLineV.setText("V5max:"+cr2+" ("+vhighest+")")



    //Module:Sub Outs5


    ObjectUtils.debugLog(TAG,"outs5 flop cardx="+acr(0)+"/"+acr(1)+"/"+acr(2)+"/"+acr(3)+"/"+acr(4))


    //outs5 calls the ndk 6-hand outs
    val racro5u:Array[Int] = coreEngine("outs5","",acr(0),acr(1),acr(2),acr(3),acr(4),0,0)

    //8-aug-14 split array into racroX and oracroX (outs id) racroX operates as normal
    //copy(src: AnyRef, srcPos: Int, dest: AnyRef, destPos: Int, length: Int): Unit

    //val racro5 = Array.ofDim[Int](1325)
    val NDKOUTLEN=52
    val racro5:Array[Int] = Array.fill[Int](NDKOUTLEN)(0)
    copy(racro5u, 0,racro5, 0, NDKOUTLEN)

/* 8-aug-14 TODO mutually exclusive outs

    val racro5o:Array[Int] = Array.fill[Int](NDKOUTLEN)(0)
    copy(racro5u, NDKOUTLEN ,racro5o, 0, NDKOUTLEN)
    //vfy
    Log.d(TAG,"racro5end="+racro5(51)+" racro5o begin="+racro5o(0))

    //8-aug-14 determine no of outs from racroXu
    val tindex:Array[Int]=Array.fill[Int](52)(0)
    val tcards:Array[Int]=Array.fill[Int](52)(0)
    var ocurrentCardSet:String=""
    for (i <- 0 to racro5o.length-1) {

      if ((racro5(i) !=IGNOREDCARD) &&  ((racro5(i) < hero_high) && (cry.htableTable_3(racro5(i)-1) != ocurrentCardSet)) && (cry.htableTable_2(racro5(i) - 1) != cry.htableTable_2(hero_high)) ) //ignore seq. same sets i.e K K K A T of different suites also ignore lowest same set with higher hero_high id, match by Value string
      {
      Log.d(TAG,""+i+" uout="+cry.htableTable_3(racro5(i)-1)+" oout="+PokerAlgs.getCardValue(racro5o(i)))

      tindex( PokerAlgs.getCardValueIndex(racro5o(i)) )+=1 //incr unique outs
      tcards(i)=racro5o(i)

      ocurrentCardSet=cry.htableTable_3(racro5(i)-1)
      }
    }
    for (i <- 0 to 51) {Log.d(TAG,"racro5 unique factored outs are "+i+" = "+PokerAlgs.getCardValue(racro5o(i)) +" tcards="+tcards(i)+" tindex="+tindex(i))}

*/



    scala.util.Sorting.quickSort(racro5) //sort ascending
    var hohighest5:String="error"
    val cr3 = racro5(0)
    var hcro=0
    var hero_highO=0

    hohighest5=ObjectUtils.returnHand(cr3)

    //2-8-14
    //max outs of best hand TODO
    //0=our first out


    //3-8-14
    //the problem with this is it also returns hands that better the hand even if not in our preflop
    //we should keep these but highlight them/dim them appropriately
    //we should move this into an objectutils routine so can also be used by mtable


    var ttouts=0
    for (i <- 0 to 20) {
      if (ObjectUtils.returnHand(racro5(i)).equals(ObjectUtils.returnHand(racro5(0)))) { //get th outs of the best hand (equality via actual description such as "A pair" via returnHand
        //Log.d(TAG, "racro5(i)=" + racro5(i) + " i=" + i + " max=" + ObjectUtils.returnHand(racro5(0)) + " matches " + ObjectUtils.returnHand(racro5(i))+" hand="+cry.htableTable_3(racro5(i)-1))
        ttouts += 1
      }
    }



    val toutsoddst:Float = ((52-5/racro5.length)) //err=1325!  TODO!

    val toutsodds:Float = ((52-5)/ttouts)
    //Log.d(TAG,"racro5 len="+racro5.length)


    val vComboO:String = cry.htableTable_2(cr3-1)
    val vCombob0:String = cry.htableTable_3(cr3-1)

    //val hiStatLineO5=findViewById(R.id.heroOuts5).asInstanceOf[TextView]
    //hiStatLineO5.setText("H5outs:"+cr3+" ("+hohighest5+")"+" "+vComboO+"/"+vCombob0)

    //calculate outs at turn
    val houtsArray:Array[Int] = Array.fill[Int](7462)(0)     // 7,462  # possible equiv classes for 5-card hands


    //hero outs at turn
    var hcount:Int=0
    var tcount:Int=0

    //1-8-14
    val outsNo:Array[Int] = Array.ofDim(100) //max no of outs expected TODO 1-8-14
    var outsCount=0

    var currentCardSet:String=""
    val cfArray:Array[Int] = Array.fill[Int](7462)(0) //never this no of outs but be safe initially, could use a list etc


    //In determining hero outs at turn - disregard high cards?
    //at the moment a kludge to disregard the first high card.... 23-Jul-14


    //first run counts the outs, same as below except we are just interested in the count
    for (houts <- racro5) {
      //display only if out is better than our current cards

      if (houts < hero_high) cfArray(houts) += 1 //correlate directly to outs mash index

      //error many 6186 A K Q J 8 in certain circumstances i.e A 6 T 3 2, this is the first high card i.e. > 6185
      //this is likely to do with the ndk outs5 eval_6 alg args?


    }


//31-Jul-14 modify the current hero outs
//racro5 = sorted val racro5:Array[Int] = coreEngine("outs5","",acr(0),acr(1),acr(2),acr(3),acr(4),0,0)
//i.e. all outs for hero flop hand

    //TODO run as async task and write out once complete -same for other lv


    val outsList  =mutable.MutableList[Int]()
    //second run gets everything else
    for (houts <- racro5) {

                          //display only if out is better than our current cards
                          //if a better out exists
                          if ((houts !=IGNOREDCARD) &&  ((houts < hero_high) && (cry.htableTable_3(houts-1) != currentCardSet)) && (cry.htableTable_2(houts - 1) != cry.htableTable_2(hero_high)) ) //ignore seq. same sets i.e K K K A T of different suites also ignore lowest same set with higher hero_high id, match by Value string
                          {

                            //24-aug-14 related to outs5/6 fix where upper bound less than 52 this should be set at 50 max, 53 to 50
                            //however giving it varies to be safe we just make sure less than 6186 which is what would be in the array default
                            //also a 6186 high card check (although already done in cEngine we set array initialiser at 6186...) TODO
                            if (houts<6186) {
                                if (cfArray(houts)<53) //TODO bug akqj9 1273 invalid outs comes from high card crossover.... bandaid fix
                                {
                                  //Log.d(TAG, "houts="+houts+" houts at flop='" + ObjectUtils.returnHand(houts) + " cr2=" + cr2 + " '" + cry.htableTable_2(houts - 1) + "' " + cry.htableTable_3(houts - 1) + " outs=" + cfArray(houts))
                                  outsList += houts

                                  outsNo(outsCount) = cfArray(houts)
                                  outsCount+=1

                                }

                            } //24-aug-14
                              currentCardSet=cry.htableTable_3(houts-1)
                          }
                        }

                      //Log.d(TAG,"outsList="+outsList)

    //31-7-14 pass over villain pf cards from t/r
    val mLv2 = new LvHeroOuts
    adapt = mLv2.oA(this, outsList.toArray, outsNo, cry)
    val lv = findViewById(R.id.vlistView2).asInstanceOf[ListView]
    lv.setOnItemClickListener(new OnItemClickListener() {
      def onItemClick(parent: AdapterView[_], view: View, position: Int, id: Long) {

        ObjectUtils.showToast("handler", getBaseContext)

      }
    })
    lv.setAdapter(adapt)



    //3-8-14 pass over cards to mash
    //set mash up for the flip construction
    //03-Aug-14
    //tb processed at outs5 outs6
    val tb:TableLayout= fl.doTable(this, outsList.toArray, outsNo, cry, 1)
    val f2:FrameLayout= findViewById(R.id.flipper2).asInstanceOf[FrameLayout]
    f2.removeView(tb)
    f2.addView(tb)








    //TODO: 25-jan-14 write out the cards

    //Module: Write out Mini Cards Hero Best Hand
    val imageIDs:Array[Int] = ObjectUtils.cardImageLookup(hCombob)
    for (z <- imageIDs) ObjectUtils.debugLog(TAG, "writing pf  image id:"+z)




    //write out pf-f card results line
    //could loops but need to change 2 vars
      System.gc

      findViewById(R.id.flop_list).asInstanceOf[LinearLayout].setBackgroundDrawable(null)

      findViewById(R.id.cardView1).asInstanceOf[ImageView].setImageResource(imageIDs(0))
      findViewById(R.id.cardView2).asInstanceOf[ImageView].setImageResource(imageIDs(1))
      findViewById(R.id.cardView3).asInstanceOf[ImageView].setImageResource(imageIDs(2))
      findViewById(R.id.cardView4).asInstanceOf[ImageView].setImageResource(imageIDs(3))
      findViewById(R.id.cardView5).asInstanceOf[ImageView].setImageResource(imageIDs(4))

      findViewById(R.id.cardView6).asInstanceOf[ImageView].setImageResource(R.drawable.red_back)
      findViewById(R.id.cardView7).asInstanceOf[ImageView].setImageResource(R.drawable.red_back)




    /*
    if (cr2>0) {
    val vCombo:String = cry.htableTable_2(cr2-1)  //TODO: -1 error on 0-1 Royal Flush see above....
    val vCombob:String = cry.htableTable_3(cr2-1)

    val hiStatLineV=findViewById(R.id.hiflopvillain).asInstanceOf[TextView]
    hiStatLineV.setText("Villain Flop Max: #"+vhighest+" ("+vCombo+")"+" "+vCombob)

    } else ObjectUtils.debugLog(TAG, "vCombo return error is:"+cr2)
    */

    /*
    val vCombo2:String = cry.htableTable_2(cr3-1)
    val vCombo2b:String = cry.htableTable_3(cr3-1)
    */


  } //displayAlgRanking5





  //Module: River Ranking for Hero
  def displayAlgRanking67(nOc:Int){

    var typeOfHand:String=""

    val acr = PokerAlgs.htable(ObjectUtils.currentPlayerCardsArrayDesc)

    for (hc <- acr) ObjectUtils.debugLog(TAG,"ndk cpcad="+hc) //debug


    //NDK Mapping
    /*
    jint c5,        acr(0)   pf1
    jint c4,        acr(1)   pf2
    jint c3,        acr(2)   flop 1
    jint c2,        acr(3)   flop 2
    jint c1,        acr(4)   flop 3

    jint c6,        acr(5)   river
    jint c7)        acr(6)   turn (reversed)
    */

    //cards passed to ndk
    if (nOc == 6) ObjectUtils.debugLog(TAG,"hero6 ndk t6="+acr(0)+"/"+acr(1)+"/"+acr(2)+"/"+acr(3)+"/"+acr(4)+"/"+acr(5))
    if (nOc == 7) ObjectUtils.debugLog(TAG,"ndk r7="+acr(0)+"/"+acr(1)+"/"+acr(2)+"/"+acr(3)+"/"+acr(4)+"/"+acr(5)+"/"+acr(6))

    //Log.d(TAG," A transfer via binder..")
    mService.be(acr,prefs.getInt("strict-equity",1))

    var racr67:Array[Int]=null
    if (nOc == 6) {
      racr67 = coreEngine("hero6","",0,acr(0),acr(1),acr(2),acr(3),acr(4),acr(5)); typeOfHand="turn"
    }

    if (nOc == 7) {racr67 = coreEngine("hero7","",acr(0),acr(1),acr(2),acr(3),acr(4),acr(5),acr(6)); typeOfHand="river"}



    var hhighest:String="error"
    val cr = racr67(0)

    hhighest= ObjectUtils.returnHand(cr)


    //Log.d(TAG,"hero6/7 hhighest ("+cr+") ="+hhighest)

    //TODO 15-2-14 V6

    //if (nOc == 6) {                               //t         f           f           f         pf          pf        r
      ObjectUtils.debugLog(TAG,"turn/river cardx="+acr(0)+"/"+acr(1)+"/"+acr(2)+"/"+acr(3)+"/"+acr(4)+"/"+acr(5)+"/"+acr(6))
      var racr67v:Array[Int]=null //=Array.fill[Int](25000)(0)

    //27-7-14
      var racr67f:Array[Int]=null //=Array.fill[Int](25000)(0)


      //27-7-14
      if (nOc == 6) {
        racr67v = coreEngine("villain6", "", 0, acr(0), acr(1), acr(2), acr(3), acr(4), acr(5))
        racr67f = coreEngine("villain6", "preflop", 0, acr(0), acr(1), acr(2), acr(3), acr(4), acr(5))
        //ObjectUtils.showToast("t6 size of range/flop "+racr67v.length+"/"+racr67f.length,this)
      }


      //27-7-14
      if (nOc == 7) {
        racr67v = coreEngine("villain7", "", acr(0), acr(1), acr(2), acr(3), acr(4), acr(5), acr(6))
        racr67f = coreEngine("villain7", "preflop", acr(0), acr(1), acr(2), acr(3), acr(4), acr(5), acr(6))
        //ObjectUtils.showToast("r7 size of range/flop "+racr67v.length+"/"+racr67f.length,this)
      }


      //27-7-14
      val MAX_PREFLOP_RETURN=racr67v.length
      var racr67x = ofDim[Int](MAX_PREFLOP_RETURN,3) //=Array.fill[Int](25000)(0)

      //27-7-14
      //amalgamate the arrays
      var racridx:Int=0
      var pfidx=0
      for ( racridx <- 0 to MAX_PREFLOP_RETURN-1){

        //unsorted hand
        racr67x(racridx)(0)=racr67v(racridx)
        //associated flop cards
        racr67x(racridx)(1)=racr67f(pfidx)
        racr67x(racridx)(2)=racr67f(pfidx+1)

        pfidx+=2

      }

      //http://stackoverflow.com/questions/22912851/scala-2d-array-sorting-by-specified-column
      racr67x = racr67x.sortWith(_(0) < _(0))


      scala.util.Sorting.quickSort(racr67v) //sort ascending


      doVillainRange67(racr67x,cr) //write villain range
      //doVillainRange5(racr67v,cr) //write villain range

      var vhhighest:String="error"
      val vcr = racr67v(0)


    vhhighest= ObjectUtils.returnHand(vcr)


//22-07-14 added
    //log all possibles and place in an array 10 counter - for use as a hand range for pre-flop
    var la:Int=0
    val hc:Array[Int] = Array.fill[Int](racr67v.length)(0)

    try {

    for (ra <- racr67v) {
      ObjectUtils.debugLog(TAG,""+la+" best is "+ra)


      //var hand:String = cry.htableTable_3(ra)

      ra match {

        case ra if (ra > 6185) => hc(10)+=1     // 1277 high card
        case ra if (ra > 3325) => hc(9)+=1      // 2860 one pair
        case ra if (ra > 2467) => hc(8)+=1      //  858 two pair
        case ra if (ra > 1609) => hc(7)+=1      //  858 three-kind
        case ra if (ra > 1599) => hc(6)+=1      //   10 straights
        case ra if (ra > 322)  => hc(5)+=1      // 1277 flushes
        case ra if (ra > 166)  => hc(4)+=1      //  156 full house
        case ra if (ra > 10)   => hc(3)+=1      //  156 four-kind
        case ra if (ra >1)     => hc(2)+=1     //   10 straight-flushes
        case ra if (ra == 1)   => hc(1)+=1   //RF

      } //ra
      la+=1

    } //range loop

    }
    catch {
      case e: Exception => {
        //Log.wtf(TAG,""+e.printStackTrace)
        Toast.makeText(this, "ra err", Toast.LENGTH_LONG).show()
      }
    }

    //for (vidx <- 1 to 10) ObjectUtils.debugLog(TAG,""+vidx+" available hands are "+hc(vidx))

//22-07-14 need hero actual
    //displayHandRange(hc, 0)  //pass Villain range and hero actual
//26-07-14
    //arg2 is a ranking 1..10


    /*
    08-16 02:54:02.154    6711-6711/com.droidroid.PM2 D/NDK_AndroidNDK1Activity﹕ NDK:LC:  Entered cEngine
    08-16 02:54:02.154    6711-6711/com.droidroid.PM2 D/NDK_AndroidNDK1Activity﹕ NDK:LC:  Exit cEngine hero6
    08-16 02:54:02.154    6711-6711/com.droidroid.PM2 D/NDK_AndroidNDK1Activity﹕ NDK:LC:  Entered cEngine
    08-16 02:54:02.154    6711-6711/com.droidroid.PM2 D/NDK_AndroidNDK1Activity﹕ NDK:LC:  Entered cEngine villain6
    08-16 02:54:02.154    6711-6711/com.droidroid.PM2 D/NDK_AndroidNDK1Activity﹕ NDK:LC:  Exit cEngine villain6
    08-16 02:54:02.154    6711-6711/com.droidroid.PM2 D/NDK_AndroidNDK1Activity﹕ NDK:LC:  Entered cEngine
    08-16 02:54:02.154    6711-6711/com.droidroid.PM2 D/NDK_AndroidNDK1Activity﹕ NDK:LC:  Entered cEngine villain6
    08-16 02:54:02.154    6711-6711/com.droidroid.PM2 D/NDK_AndroidNDK1Activity﹕ NDK:LC:  Exit cEngine villain6 pf
    ...

    A/LvVillainRange﹕ heroToHand fed rubbish! (heroToHand) -- from gSA - ultimately hh frm ndk call, hh would have been 0

    ...

    08-16 01:13:14.974  32463-32463/com.droidroid.PM2 E/AndroidRuntime﹕ FATAL EXCEPTION: main
    Process: com.droidroid.PM2, PID: 32463
    b.x: 0 (of class java.lang.Integer)

    08-16 02:54:03.966    6711-6711/com.droidroid.PM2 E/AndroidRuntime﹕ FATAL EXCEPTION: main
    Process: com.droidroid.PM2, PID: 6711
    b.x: 0 (of class java.lang.Integer)
     */
try {
  var hhand: Int = 0
  cr match {

    case cr if (cr > 6185) => hhand = 10 // 1277 high card
    case cr if (cr > 3325) => hhand = 9 // 2860 one pair
    case cr if (cr > 2467) => hhand = 8 //  858 two pair
    case cr if (cr > 1609) => hhand = 7 //  858 three-kind
    case cr if (cr > 1599) => hhand = 6 //   10 straights
    case cr if (cr > 322) => hhand = 5 // 1277 flushes
    case cr if (cr > 166) => hhand = 4 //  156 full house
    case cr if (cr > 10) => hhand = 3 //  156 four-kind
    case cr if (cr > 1) => hhand = 2 //   10 straight-flushes
    case cr if (cr == 1) => hhand = 1 //RF

  }
  displayHandRange(hc, hhand) //TODO!!!
}
catch {
  case e: Exception => {
    //Log.wtf(TAG,""+e.printStackTrace)
    Toast.makeText(this, "cr err", Toast.LENGTH_LONG).show()
  }
}




//1-8-14 test of Hero6


    //outs6
    if (nOc == 6) {
    val racro6u:Array[Int] = coreEngine("outs6", "", 0, acr(0), acr(1), acr(2), acr(3), acr(4), acr(5))

      //8-aug-14 split array into racroX and oracroX (outs id) racroX operates as normal
      //copy(src: AnyRef, srcPos: Int, dest: AnyRef, destPos: Int, length: Int): Unit

      //val racro6 = Array.ofDim[Int](1325)
      val NDKOUTLEN=52
      val racro6:Array[Int] = Array.fill[Int](NDKOUTLEN)(0)
      copy(racro6u, 0,racro6, 0, NDKOUTLEN)


/* 8-aug-14 TODO mutually exclusive outs
      val racro6o:Array[Int] = Array.fill[Int](NDKOUTLEN)(0)
      copy(racro6u, NDKOUTLEN ,racro6o, 0, NDKOUTLEN)
      //vfy
      Log.d(TAG,"racro6end="+racro6(51)+" racro6o begin="+racro6o(0))
      val tindex:Array[Int]=Array.fill[Int](52)(0)
      val tcards:Array[Int]=Array.fill[Int](52)(0)

      //8-aug-14 determine no of outs from racroXu
      var ocurrentCardSet:String=""
      for (i <- 0 to racro6o.length-1) {
      //6186=highcard
        if ((racro6(i) !=IGNOREDCARD) &&  ((racro6(i) < 6186) && (cry.htableTable_3(racro6(i)-1) != ocurrentCardSet)) && (cry.htableTable_2(racro6(i) - 1) != cry.htableTable_2(6186)) ) //ignore seq. same sets i.e K K K A T of different suites also ignore lowest same set with higher hero_high id, match by Value string
        {
          Log.d(TAG,""+i+" uout="+cry.htableTable_3(racro6(i)-1)+" oout="+PokerAlgs.getCardValue(racro6o(i)))

          tindex( PokerAlgs.getCardValueIndex(racro6o(i)) )+=1 //incr unique outs
          tcards(i)=racro6o(i)

          ocurrentCardSet=cry.htableTable_3(racro6(i)-1)
        }
      }
      for (i <- 0 to 51) {Log.d(TAG,"racro6 unique factored outs are "+i+" = "+PokerAlgs.getCardValue(racro6o(i)) +" tcards="+tcards(i)+" tindex="+tindex(i))}

*/


    scala.util.Sorting.quickSort(racro6) //sort ascending
    //for (ra <- racro6) Log.d(TAG, "ra="+ra+" outs6="+cry.htableTable_2(ra- 1) + " " + cry.htableTable_3(ra - 1))


    //calculate outs at turn
    val houtsArray:Array[Int] = Array.fill[Int](7462)(0)     // 7,462  # possible equiv classes for 5-card hands


    //hero outs at turn
    var hcount:Int=0
    var tcount:Int=0
    //1-8-14
    val outsNo:Array[Int] = Array.ofDim(100) //max no of outs expected TODO 1-8-14
    var outsCount=0

    var currentCardSet:String=""
    val cfArray:Array[Int] = Array.fill[Int](7462)(0) //never this no of outs but be safe initially, could use a list etc


    //In determining hero outs at turn - disregard high cards?
    //at the moment a kludge to disregard the first high card.... 23-Jul-14


    //first run counts the outs, same as below except we are just interested in the count
    for (houts <- racro6) {
      //display only if out is better than our current cards

      if (houts < cr) cfArray(houts) += 1 //correlate directly to outs mash index

      //error many 6186 A K Q J 8 in certain circumstances i.e A 6 T 3 2, this is the first high card i.e. > 6185
      //this is likely to do with the ndk outs5 eval_6 alg args?


    }



    val outsList  =mutable.MutableList[Int]()
    //second run gets everything else
    for (houts <- racro6) {

      //display only if out is better than our current cards
      //if a better out exists
      //1-8-14 double check miss out any high cards due to the AKQJ9 anomoly (however an out will NEVER be a high card! Pair at worse...)
      val HIGHCARD:Int = 6186
      if ( ((houts !=IGNOREDCARD) && (houts < HIGHCARD) && (houts < cr) && (cry.htableTable_3(houts-1) != currentCardSet)) && (cry.htableTable_2(houts - 1) != cry.htableTable_2(cr)) ) //ignore seq. same sets i.e K K K A T of different suites also ignore lowest same set with higher hero_high id, match by Value string
      {

        //24-aug-14 see racro5
        if (cfArray(houts)<53) //TODO bug akqj9 1273 invalid outs comes from high card crossover.... bandaid fix
        {
          //Log.d(TAG, "6 houts="+houts+" houts at turn='" + ObjectUtils.returnHand(houts) + "' cr=" + cr + " '" + cry.htableTable_2(houts - 1) + "' " + cry.htableTable_3(houts - 1) + " outs=" + cfArray(houts))
          outsList += houts

          outsNo(outsCount) = cfArray(houts)
          outsCount+=1
        }
        currentCardSet=cry.htableTable_3(houts-1)
      }
    }

    //Log.d(TAG,"outsList="+outsList)

    //31-7-14 pass over villain pf cards from t/r
    val mLv2 = new LvHeroOuts
    adapt = mLv2.oA(this, outsList.toArray, outsNo, cry)
      val lv = findViewById(R.id.vlistView2).asInstanceOf[ListView]
      lv.setOnItemClickListener(new OnItemClickListener() {
        def onItemClick(parent: AdapterView[_], view: View, position: Int, id: Long) {

          ObjectUtils.showToast("handler", getBaseContext)

        }
      })
      lv.setAdapter(adapt)

      //3-8-14 pass over cards to mash
      //set mash up for the flip construction
      //03-Aug-14
      //tb processed at outs5 outs6
      val tb:TableLayout= fl.doTable(this, outsList.toArray, outsNo, cry,2)
      val f2:FrameLayout= findViewById(R.id.flipper2).asInstanceOf[FrameLayout]
      f2.removeView(tb)
      f2.addView(tb)

    } //noc6
    else //noc=7
    {

    //findViewById(R.id.vlistView2).asInstanceOf[ListView] ... whatever

    }




    /*
    if (nOc == 6){
      val tt=findViewById(R.id.TurnTest).asInstanceOf[TextView]
      tt.setText("V6TurnTest#:"+racr67v(0)+" ("+vhhighest+")"+" "+cry.htableTable_3(vcr-1))
      }

      if (nOc == 7){
      val tt=findViewById(R.id.RiverTest).asInstanceOf[TextView]
      tt.setText("V7RiverTest#:"+racr67v(0)+" ("+vhhighest+")"+" "+cry.htableTable_3(vcr-1))
      }
    */
    //}


              /*
              //turn test
              var thhighest:String="error"
              var racrt:Array[Int]=null

              //args passed in reverse
              ObjectUtils.debugLog(TAG,"turn/river cardx="+acr(0)+"/"+acr(1)+"/"+acr(2)+"/"+acr(3)+"/"+acr(4)+"/"+acr(5)+"/"+acr(6))

              //passed in 5=pf2 6=pf1
              //0=river
              racrt = coreEngine("hero6",0,acr(1),acr(2),acr(3),acr(4),acr(5),acr(6))
              val tcr = racrt(0)

              tcr match {

                case cr if (tcr > 6185) => thhighest="10. High Card"      // 1277 high card
                case cr if (tcr > 3325) => thhighest="9. One Pair"       // 2860 one pair
                case cr if (tcr > 2467) => thhighest="8. Two Pair"       //  858 two pair
                case cr if (tcr > 1609) => thhighest="7. Three of a Kind"//  858 three-kind
                case cr if (tcr > 1599) => thhighest="6. Straight"       //   10 straights
                case cr if (tcr > 322)  => thhighest="5. Flush"           // 1277 flushes
                case cr if (tcr > 166)  => thhighest="4. Full House 3+2"      //  156 full house
                case cr if (tcr > 10)   => thhighest="3. Four of a Kind"   //  156 four-kind
                case cr if (tcr>1)      => thhighest="2. Straight Flush"      //   10 straight-flushes
                case cr if (tcr== 1)      => thhighest="1. Royal Flush"
                case cr if (tcr ==0)      => thhighest="H-LO-ER Royal Flush?"   //TODO not sure if this error ever raised

              }
              val tt=findViewById(R.id.TurnTest).asInstanceOf[TextView]
              tt.setText("TurnTest:#"+racrt(0)+" ("+thhighest+")")
             */


    /*
    val cr2:Int = coreEngine("villain",acr(0),acr(1),acr(2),acr(3),acr(4))
    var vhighest:String="error"

    cr2 match {

      case cr2 if (cr2 > 6185) => vhighest="10. High Card"      // 1277 high card
      case cr2 if (cr2 > 3325) => vhighest="9. One Pair"       // 2860 one pair
      case cr2 if (cr2 > 2467) => vhighest="8. Two Pair"       //  858 two pair
      case cr2 if (cr2 > 1609) => vhighest="7. Three of a Kind"//  858 three-kind
      case cr2 if (cr2 > 1599) => vhighest="6. Straight"       //   10 straights
      case cr2 if (cr2 > 322)  => vhighest="5. Flush"           // 1277 flushes
      case cr2 if (cr2 > 166)  => vhighest="4. Full House 3+2"      //  156 full house
      case cr2 if (cr2 > 10)   => vhighest="3. Four of a Kind"   //  156 four-kind
      case cr2 if (cr2>1)      => vhighest="2. Straight Flush"      //   10 straight-flushes
      case cr2 if (cr2== 1)      => vhighest="1. Royal Flush"
      case cr2 if (cr2==0)      => {  vhighest="V-LO-ER (Royal) Flush?" }  //TODO fix for ndkalg return 0 on rf

    }

    val cr3:Int = coreEngine("villain2",acr(0),acr(1),acr(2),acr(3),acr(4))
    var vhighest2:String="error"

    cr3 match {

      case cr3 if (cr3 > 6185) => vhighest2="10. High Card"      // 1277 high card
      case cr3 if (cr3 > 3325) => vhighest2="9. One Pair"       // 2860 one pair
      case cr3 if (cr3 > 2467) => vhighest2="8. Two Pair"       //  858 two pair
      case cr3 if (cr3 > 1609) => vhighest2="7. Three of a Kind"//  858 three-kind
      case cr3 if (cr3 > 1599) => vhighest2="6. Straight"       //   10 straights
      case cr3 if (cr3 > 322)  => vhighest2="5. Flush"           // 1277 flushes
      case cr3 if (cr3 > 166)  => vhighest2="4. Full House 3+2"      //  156 full house
      case cr3 if (cr3 > 10)   => vhighest2="3. Four of a Kind"   //  156 four-kind
      case cr3 if (cr3>1)      => vhighest2="2. Straight Flush"      //   10 straight-flushes
      case cr3 if (cr3== 1)      => vhighest2="1. Royal Flush"
      case cr3 if (cr3==0)      =>  vhighest2="V-LO-ER (Royal) Flush?" //TODO fix for ndkalg return 0 on rf

    }
    */
    var opth,optv:String=""

    //fix 22-7-14
    var tcr=cr
    if (cr==0) tcr=1

    val hCombo:String = cry.htableTable_2(tcr-1)    //TODO: error on cr==0 , this 0 returned from ndkalg.c in certain flush/royal circumstances?
    val hCombob:String = cry.htableTable_3(tcr-1)
    /*
    val vCombo:String = cry.htableTable_2(cr2-1)  //TODO: -1 error on 0-1 Royal Flush see above....
    val vCombob:String = cry.htableTable_3(cr2-1)

    val vCombo2:String = cry.htableTable_2(cr3-1)
    val vCombo2b:String = cry.htableTable_3(cr3-1)
    */

   //gc
   System.gc()


    /*
    02-10 21:19:11.345: ERROR/AndroidRuntime(19973): FATAL EXCEPTION: main
        Process: com.droidroid.PM2, PID: 19973
        java.lang.OutOfMemoryError

        due to bitmap resource issues
     */



    //TODO issues here with 6 cards
   //temporary view set up
   //val hiStatLine2=findViewById(R.id.MiniStatusBar).asInstanceOf[TextView]
   //hiStatLine2.setText("Hero Turn/River: #"+hhighest+" ("+hCombo+")"+" "+hCombob)

    //fix 31-Jul-14
    val hiStatLineH=findViewById(R.id.MiniStatusBar).asInstanceOf[TextView]
    hiStatLineH.setText(""+typeOfHand+": #"+hhighest+" ("+hCombo+")"+" "+hCombob)
    findViewById(R.id.bigStatusLine).asInstanceOf[TextView].setText(typeOfHand+": #"+hhighest+" ("+hCombo+")"+" "+hCombob)



    //TODO issues here with 6 cards
    val imageIDs:Array[Int] = ObjectUtils.cardImageLookup(hCombob)
    for (z <- imageIDs) ObjectUtils.debugLog(TAG, "writing pf/flop/turn/river image id:"+z)


    //write out pf-f-t-r card results line
    //could loops but need to change 2 vars


    findViewById(R.id.cardView1).asInstanceOf[ImageView].setImageResource(imageIDs(0))
    findViewById(R.id.cardView2).asInstanceOf[ImageView].setImageResource(imageIDs(1))
    findViewById(R.id.cardView3).asInstanceOf[ImageView].setImageResource(imageIDs(2))
    findViewById(R.id.cardView4).asInstanceOf[ImageView].setImageResource(imageIDs(3))
    findViewById(R.id.cardView5).asInstanceOf[ImageView].setImageResource(imageIDs(4))
    //if (nOc > 5)
      findViewById(R.id.cardView6).asInstanceOf[ImageView].setImageResource(imageIDs(5))   //turn card
    if (nOc == 7)
      findViewById(R.id.cardView7).asInstanceOf[ImageView].setImageResource(imageIDs(6))   //river card
    else
      findViewById(R.id.cardView7).asInstanceOf[ImageView].setImageResource(R.drawable.red_back)
    //TODO memory error here      - recycle ()?


  } //displayAlgRanking67




  //display hand range 5/6/7

  def displayHandRange(tR:Array[Int],tH:Int) {

    //achartengine core support
    val rrenderer: XYMultipleSeriesRenderer = rgetPfMyBarRenderer
    rmyPfChartSettings(rrenderer)

    val rmChartView = ChartFactory.getBarChartView(getApplicationContext(), rgetPfMyBarDataset(tR,tH), rrenderer,Type.STACKED)
    val rlayout = findViewById(R.id.rchart).asInstanceOf[FrameLayout]

    //19-Jul-14
    rlayout.removeAllViews()

    rlayout.addView(rmChartView)
    rmChartView.refreshDrawableState()

    //19-Jul-14 border control
    rlayout.setBackgroundResource(R.drawable.customborders_redv2)
    findViewById(R.id.chart).asInstanceOf[LinearLayout].setBackgroundResource(R.drawable.customborders_redv2)
    findViewById(R.id.status_view).asInstanceOf[LinearLayout].setBackgroundResource(R.drawable.customborders_redv2)
    findViewById(R.id.mypos).asInstanceOf[FrameLayout].setBackgroundResource(R.drawable.customborders_redv2)

  }



  private def rgetPfMyBarDataset(tR:Array[Int],tH:Int): XYMultipleSeriesDataset = {
    rPfbuildInitialRange(tR,tH)
  }



  def rgetPfMyBarRenderer: XYMultipleSeriesRenderer = {
    val rrenderer: XYMultipleSeriesRenderer = new XYMultipleSeriesRenderer

    if (device_width<=1280) rrenderer.setAxisTitleTextSize(15) else rrenderer.setAxisTitleTextSize(25)
    if (device_width<=1280) rrenderer.setChartTitleTextSize(15) else rrenderer.setChartTitleTextSize(25)
    if (device_width<=1280) rrenderer.setLabelsTextSize(15) else rrenderer.setLabelsTextSize(25)
    if (device_width<=1280) rrenderer.setLegendTextSize(15) else rrenderer.setLegendTextSize(25)
    //rrenderer.setLegendHeight(-5)

    rrenderer.setMargins(Array[Int](0, 4, 23, 14))  //tlbr
    rrenderer.setDisplayValues(true)

    //java.lang.ClassCastException: org.achartengine.renderer.SimpleSeriesRenderer cannot be cast to org.achartengine.renderer.XYSeriesRenderer
    //1.1 to 1.2 changed from simpleseries to xyseries
    val rr: XYSeriesRenderer = new XYSeriesRenderer
    rr.setColor(Color.RED)
    rr.setDisplayChartValues(true)
    if (device_width<=1280) rr.setChartValuesTextSize(20) else rr.setChartValuesTextSize(45)
    rrenderer.addSeriesRenderer(rr)


    val rr2: XYSeriesRenderer = new XYSeriesRenderer
    rr2.setColor(Color.YELLOW)
    rr2.setDisplayChartValues(true)
    if (device_width<=1280) rr2.setChartValuesTextSize(20) else rr2.setChartValuesTextSize(45)
    rrenderer.addSeriesRenderer(rr2)


    val rr3: XYSeriesRenderer = new XYSeriesRenderer
    rr3.setColor(Color.GREEN)
    rr3.setDisplayChartValues(true)
    if (device_width<=1280) rr3.setChartValuesTextSize(20) else rr3.setChartValuesTextSize(45)
    rrenderer.addSeriesRenderer(rr3)

    rrenderer
  }

  private def rmyPfChartSettings(rrenderer: XYMultipleSeriesRenderer) {

    if (device_width<=1280) {
      rrenderer.setChartTitle("Hand Range - 1326")
      rrenderer.setBarWidth(18f)
    }
    else {
      rrenderer.setChartTitle("Hand -Flop/Turn/River- Range - 1326")
      rrenderer.setBarWidth(35f) //width of each bar in pixels
    }

    rrenderer.setBarSpacing(0.1) //in bar charts

    //deprecated 1.1 methods unavailable in 1.2
    //rrenderer.setDisplayChartValues(true) //need to see Villains capabilities
    //rrenderer.setChartValuesTextSize(30)


    //horiz
    rrenderer.setXLabelsAlign(Align.CENTER)
    rrenderer.setXAxisMin(0)
    rrenderer.setXAxisMax(10)
    rrenderer.setXLabels(0)
    //renderer.setXLabelsAngle(35)



    //vert
    rrenderer.setYLabels(6) //10-aug-14
    rrenderer.setYLabelsAlign(Align.RIGHT)
    rrenderer.setYAxisMin(1)
    rrenderer.setYAxisMax(1000)
    //rrenderer.setYLabels(4)
    //rrenderer.setYLabelsAngle(45)


    //tlbr
    //val mg:Array[Int]=Array(15, 15, 5, 0)
    //renderer.setMargins(mg)

    rrenderer.addXTextLabel(1, "hc")
    rrenderer.addXTextLabel(2, "1P")
    rrenderer.addXTextLabel(3, "2P")
    rrenderer.addXTextLabel(4, "3K")
    rrenderer.addXTextLabel(5, "St")
    rrenderer.addXTextLabel(6, "Fl")
    rrenderer.addXTextLabel(7, "3+2")
    rrenderer.addXTextLabel(8, "4K")
    rrenderer.addXTextLabel(9, "SF")
    rrenderer.addXTextLabel(10, "RF")

    rrenderer.setShowLabels(true)

    //val margins: Array[Int] = Array(0, 0, 0, 0)
    //rrenderer.setMargins(margins)


    rrenderer.setApplyBackgroundColor(true)
    rrenderer.setGridColor(Color.argb(0x00, 0x01, 0x01, 0x01))
    rrenderer.setMarginsColor(Color.argb(0x00, 0x01, 0x01, 0x01))
    rrenderer.setBackgroundColor(Color.argb(0x00, 0x01, 0x01, 0x01))

    //renderer.setXTitle("[Alg:Equity vs Random]")
    //rrenderer.setYTitle("possible")
    rrenderer.setShowGrid(true)
    rrenderer.setGridColor(Color.GRAY)

  }

  private def rPfbuildInitialRange(tR:Array[Int],tH:Int): XYMultipleSeriesDataset  = {

    val rdataset: XYMultipleSeriesDataset = new XYMultipleSeriesDataset

    val legendTitles: util.ArrayList[String] = new util.ArrayList[String]

    //tR holds Villain Range (hc), tH holds hero actual
    //this is 1..10 with 1 being rf, 10 being hc

    //ObjectUtils.showToast("Updating Hand Range BarGraph ",getBaseContext)

    //Log.d(TAG,"tH="+tH)


    //Villain lo yellow
    var title:String="V-Hi"
    if (device_width<=1280) title="VH" else title="Opponent-H"
    val villain_lo_series: XYSeries = new XYSeries (title)

    if (tR(10)> 0 && (tH > 10)) villain_lo_series.add(1,tR(10))
    if (tR(9)> 0 && (tH > 9)) villain_lo_series.add(2,tR(9))
    if (tR(8)> 0 && (tH > 8)) villain_lo_series.add(3,tR(8))
    if (tR(7)> 0 && (tH > 7)) villain_lo_series.add(4,tR(7))
    if (tR(6)> 0 && (tH > 6)) villain_lo_series.add(5,tR(6))
    if (tR(5)> 0 && (tH > 5)) villain_lo_series.add(6,tR(5))
    if (tR(4)> 0 && (tH > 4)) villain_lo_series.add(7,tR(4))
    if (tR(3)> 0 && (tH > 3)) villain_lo_series.add(8,tR(3))
    if (tR(2)> 0 && (tH > 2)) villain_lo_series.add(9,tR(2))
    if (tR(1)> 0 && (tH > 1)) villain_lo_series.add(10,tR(1))

    rdataset.removeSeries(villain_lo_series)
    rdataset.addSeries(villain_lo_series)



    //Villain hi red
    title="V-Hi"
    if (device_width<=1280) title="V-Lo" else title="Opponent-L"
    val villain_hi_series: XYSeries = new XYSeries(title)

    if (tR(10)>0 && (tH < 10)) villain_hi_series.add(1,tR(10))
    if (tR(9)>0 && (tH < 9)) villain_hi_series.add(2,tR(9))
    if (tR(8)>0 && (tH < 8)) villain_hi_series.add(3,tR(8))
    if (tR(7)>0 && (tH < 7)) villain_hi_series.add(4,tR(7))
    if (tR(6)>0 && (tH < 6)) villain_hi_series.add(5,tR(6))
    if (tR(5)>0 && (tH < 5)) villain_hi_series.add(6,tR(5))
    if (tR(4)>0 && (tH < 4)) villain_hi_series.add(7,tR(4))
    if (tR(3)>0 && (tH < 3)) villain_hi_series.add(8,tR(3))
    if (tR(2)>0 && (tH < 2)) villain_hi_series.add(9,tR(2))
    if (tR(1)>0 && (tH < 1)) villain_hi_series.add(10,tR(1))


    rdataset.removeSeries(villain_hi_series)
    rdataset.addSeries(villain_hi_series)


    //hero green
    title="V/H"
    if (device_width<=1280) title="Opp/Player" else title="Opponent/Player"

    val hero_series: XYSeries = new XYSeries (title)

    if (tH==10) hero_series.add(1,tR(10)+1) //hc
    if (tH==9) hero_series.add(2,tR(9)+1) //1p
    if (tH==8) hero_series.add(3,tR(8)+1) //2p
    if (tH==7) hero_series.add(4,tR(7)+1) //3k
    if (tH==6) hero_series.add(5,tR(6)+1) //str
    if (tH==5) hero_series.add(6,tR(5)+1) //fl
    if (tH==4) hero_series.add(7,tR(4)+1) //3+2
    if (tH==3) hero_series.add(8,tR(3)+1) //4k
    if (tH==2) hero_series.add(9,tR(2)+1) //sf
    if (tH==1) hero_series.add(10,tR(1)+1) //rf

    rdataset.removeSeries(hero_series)
    rdataset.addSeries(hero_series)


    rdataset

  }


  def initPfChart {

    //achartengine core support
    val renderer: XYMultipleSeriesRenderer = getPfMyBarRenderer
    myPfChartSettings(renderer)

    val mChartView = ChartFactory.getBarChartView(getApplicationContext(), getPfMyBarDataset, renderer,Type.STACKED)
    val layout = findViewById(R.id.chart).asInstanceOf[LinearLayout]

    layout.setBackgroundDrawable(null)

    layout.addView(mChartView)
    mChartView.refreshDrawableState()

  }


  /*
  private def rbuildPfStrengthBarGraph {


    //achartengine core support
    val rrenderer: XYMultipleSeriesRenderer = rgetPfMyBarRenderer
    myPfChartSettings(rrenderer)

    val rbuildPfr:XYMultipleSeriesDataset  = rPfbuildInitialRange(atR,tH)

    val rmChartView = ChartFactory.getBarChartView(getApplicationContext(), rbuildPfr, rrenderer,Type.STACKED)
    val rlayout = findViewById(R.id.rchart).asInstanceOf[LinearLayout]

    rlayout.removeAllViews()
    rlayout.addView(rmChartView)

    rmChartView.repaint()

  }
  */


  def doVillainRange5(rac:Array[Int],hehi:Int)  {
    //V5 support code (in algRanking5)
    //we pass in racrX (Vx array of result id's from the ndk alg)
    var x=0
    val vR = Array.fill[String](cry.htableTable_3.length)("")   //TODO: sort proper length
    val vRi = Array.fill[Int](cry.htableTable_3.length)(0) //? what len?

    //unused but needed for func call
    val vRpf1 = Array.fill[Int](cry.htableTable_3.length)(0) //? what len?
    val vRpf2 = Array.fill[Int](cry.htableTable_3.length)(0) //? what len?

    for (s <- rac)  {
      x+=1
      ObjectUtils.debugLog(TAG,""+x+" vRs="+s) //13-8-14 debug code for abend on final wheel
      if (s > 0) {
        vR(x) = cry.htableTable_3(s-1)  // equate to string hand range   , i.e. 1234 = AAKKK (Illustrative)
        vRi(x) = s
      }

    }
    //do the writing
    villainRange(vR,vRi,hehi,vRpf1,vRpf2) //Villain Range 5 ListView vs Hero and hand type (via Int) displayed as a right side ListView
    //
  }

  //27-7-14
  def doVillainRange67(rac:Array[Array[Int]],hehi:Int)  {
    //we pass in racrX (Vx array of result id's from the ndk alg)
    var x=0
    val vR = Array.fill[String](cry.htableTable_3.length)("")   //TODO: sort proper length
    val vRi = Array.fill[Int](cry.htableTable_3.length)(0) //? what len?

    val vRpf1 = Array.fill[Int](cry.htableTable_3.length)(0) //? what len?
    val vRpf2 = Array.fill[Int](cry.htableTable_3.length)(0) //? what len?

    for (s <- rac)  {
      x+=1
      ObjectUtils.debugLog(TAG,"vRs="+s)
      if (s(0) > 0)  {
        vR(x) = cry.htableTable_3(s(0)-1)  // equate to string hand range   , i.e. 1234 = AAKKK (Illustrative)
        vRi(x) = s(0) //equate to cac value i.e. 1234

        vRpf1(x)=s(1)
        vRpf2(x)=s(2)

      }
    }
    //do the writing
    villainRange(vR,vRi,hehi,vRpf1,vRpf2) //Villain Range 5 ListView vs Hero and hand type (via Int) displayed as a right side ListView
    //
  }


  //ListView of Villain Range and Type - 27-7-14 added villain pf cards
  def villainRange(vLa:Array[String],vh:Array[Int],hh:Int,pf1:Array[Int],pf2:Array[Int]) = {

    //debug Vx highest
    var vtemp:Int=6500
    for (vvh <- vh) {if (vvh < vtemp && vvh>0) vtemp=vvh }
    //ObjectUtils.showToast("vhi="+vtemp,this)

    //get the lv/tv resource
    val lv:ListView =findViewById(R.id.vListView).asInstanceOf[ListView]
    val tv:TextView =findViewById(R.id.listofType).asInstanceOf[TextView]

    //remove the blur startup layout residue
    lv.setBackgroundResource(R.drawable.customborders_redv2)
    tv.setBackgroundResource(R.drawable.customborders_redv2)

    val mLv = new LvVillainRange   //instantiate the class that provides us the adapter pre (populated)

    //27-7-14 pass over villain pf cards from t/r

    lv.setAdapter(mLv.gSA(this,vLa,vh,hh,ObjectUtils.currentPlayerCardsArrayDesc,pf1,pf2,ek)) //set the adapter on the lv, populate with vLa. pass hh (hero_high) and pre-flop cards


    //kludge
    prefs = this.getSharedPreferences("com.droidroid.PM2", Context.MODE_PRIVATE)
    val mpos = prefs.getInt("misc0", 0)
    lv.setSelection(mpos)

    //write hand str (calc in LvVillainRange)
    val ppos = prefs.getInt("ph", 0)
    val vlo2 = prefs.getInt("pvlo", 0)
    val vhi2 = prefs.getInt("pvhi", 0)
    val vco = prefs.getInt("pvco", 0)


    val nuts = prefs.getInt("nuts", 999)
    //Log.d(TAG,"hero position ="+nuts)

    //if (nuts==1) {
    //  mFlipperView2.setAlpha(0)
    //} else mFlipperView2.setAlpha(1)


   // val hperc: TextView = findViewById(R.id.hmid).asInstanceOf[TextView]
   // val vlo: TextView = findViewById(R.id.vlo).asInstanceOf[TextView]
   // val vhi: TextView = findViewById(R.id.vhi).asInstanceOf[TextView]

    //hperc.setText("---")
    //vlo.setText(""+vlo2) //vlo hands
    //vhi.setText(""+vhi2+" "+"("+vco+")") //vhi hands


    //onclick for the lv items
    //TODO fix "java.lang.ClassCastException: android.widget.TwoLineListItem cannot be cast to android.widget.TextView"
    lv.setOnItemClickListener(new OnItemClickListener() {
      override def onItemClick(parent: AdapterView[_], view: View, position: Int, id: Long) {
        //val is:String = view.findViewById(android.R.id.text1).asInstanceOf[TextView].getText().toString()
        //Toast.makeText(getApplicationContext(),"hand="+is,Toast.LENGTH_SHORT).show()

        //we do this every call as we need the new instance fo new menu(kludge better implement a qa method for clear)
        //setUpQAH(position)
        //item handled in the quickAction overridden handler in quickActionV
        //quickActionH.show(view)
      }
    })
  }





  //TODO: equity analysis ALL-IN on the pf status panel
  //http://www.propokertools.com/help/simulator_docs#graph
  //
  //i.e. 52.3% All in Equity



/*


  //speech callbacks
  override def onBeginningOfSpeech {
    Log.d("Speech", "onBeginningOfSpeech")
  }


  override def onBufferReceived(buffer: Array[Byte]) {
    Log.d("Speech", "onBufferReceived")
  }

  override def onEndOfSpeech {
    Log.d("Speech", "onEndOfSpeech")
  }

  override def onError(error: Int) {
    Log.d("Speech", "onError")
  }

  override def onEvent(eventType: Int, params: Bundle) {
    Log.d("Speech", "onEvent")
  }

  override def onPartialResults(partialResults: Bundle) {
    Log.d("Speech", "onPartialResults")
  }

  override def onReadyForSpeech(params: Bundle) {
    Log.d("Speech", "onReadyForSpeech")
  }

  override def onRmsChanged(rmsdB: Float) {
    Log.d("Speech", "onRmsChanged")
  }


  override def onResults(results:Bundle) {
    Log.d("Speech", "onResults")
    val strlist= results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)

    //for (i <- strlist.size())
    ObjectUtils.showToast("result=" + strlist.get(0),this)
    //ObjectUtils.showToast("result=" + strlist.get(1),this)
  }

*/


  //10-aug-14
  def lockForwardIfRedBackCurrent(currentCard:Int): Unit = {

    val cc = currentCard-1 //is our actual array subscript, as we start from wv2 the arg is a minimum 2 (giving 1)
    // 1 compares to 0
    // 2 compares to 1 and 0
    // 3 compares to 2 and 1 and 0 ...etc

    var invc:Boolean=false
    for (pc <- 0 to cc-1)      //current card                              //a previous card from 0 to previous
      if (ObjectUtils.currentPlayerCardsArrayDesc(cc) == ObjectUtils.currentPlayerCardsArrayDesc(pc)) invc=true

      if (invc) {

        //display it
        val hiStatLineH=findViewById(R.id.MiniStatusBar).asInstanceOf[TextView]
        hiStatLineH.setText("--Select Valid Card--")

      Toast.makeText(getBaseContext, "Cannot proceed with an invalid card!", Toast.LENGTH_SHORT).show()

      //alpha flipper2
      mFlipperView2.setAlpha(0.3f)


      val cva = Array[Int](R.id.cardView1,R.id.cardView2,R.id.cardView3,R.id.cardView4,R.id.cardView5,R.id.cardView6,R.id.cardView7)   //TODO: sort proper length
      findViewById(cva(currentCard-1)).asInstanceOf[ImageView].setImageResource(R.drawable.red_back)

      val ca = Array[Int](R.id.card_display_1,R.id.card_display_2,R.id.card_display_3,R.id.card_display_4,R.id.card_display_5,R.id.card_display_6,R.id.card_display_7)   //TODO: sort proper length

      for (rsc <- currentCard to 6) {
        //block future qwheels
        findViewById(ca(rsc)).asInstanceOf[WheelView].setEnabled(false)
      }

    }

    //==

  } //lfirbc

  //09-Aug-14
  //TODO as rsc not updating on a redback
  def freezeOnRedback(currentCard:Int): Unit ={
  //check for redback up to current
  var rbfound:Boolean=false
    //
    //need to pull directly from wheel

  val ca = Array[Int](R.id.card_display_1,R.id.card_display_2,R.id.card_display_3,R.id.card_display_4,R.id.card_display_5,R.id.card_display_6,R.id.card_display_7)   //TODO: sort proper length

  for (rsc <- 0 to currentCard)  {
  val wvx: WheelView = findViewById(ca(rsc)).asInstanceOf[WheelView]
  if (wvx.getCurrentItem == 52) rbfound=true

    //10-aug-14 TODO also true if a previous card! need to get rsc... not position R.drawable.red_back etc
  }

  if (rbfound) {
    //Log.d(TAG,"freezing ")
    this.findViewById(R.id.LowerLinearLayout).asInstanceOf[LinearLayout].setAlpha(0.3f)
    this.findViewById(R.id.vListView).asInstanceOf[ListView].setAlpha(0.3f)



  }  else
  {//show all
    //Log.d(TAG,"unfreezing ")
    this.findViewById(R.id.LowerLinearLayout).asInstanceOf[LinearLayout].setAlpha(1f)
    this.findViewById(R.id.vListView).asInstanceOf[ListView].setAlpha(1f)

  }

  }//for

  //09-aug-14
  def trashTheHandDown(noOfCard:Int): Unit = {

    //we always trash before flop completion
    ObjectUtils.currentPlayerCardsArrayDesc(noOfCard) = ""
    ObjectUtils.currentPlayerCardsArrayRsc(noOfCard) = 0

  }


  //09-aug-14
  def statusCardsReset(ca:Int): Unit ={

    val cva = Array[Int](R.id.cardView1,R.id.cardView2,R.id.cardView3,R.id.cardView4,R.id.cardView5,R.id.cardView6,R.id.cardView7)   //TODO: sort proper length

    for (rsc <- ca to 6)  findViewById(cva(rsc)).asInstanceOf[ImageView].setImageResource(R.drawable.red_back)

    /*
    findViewById(R.id.cardView1).asInstanceOf[ImageView].setImageResource(android.R.color.transparent)
    findViewById(R.id.cardView2).asInstanceOf[ImageView].setImageResource(android.R.color.transparent)
    findViewById(R.id.cardView3).asInstanceOf[ImageView].setImageResource(android.R.color.transparent)
    findViewById(R.id.cardView4).asInstanceOf[ImageView].setImageResource(android.R.color.transparent)
    findViewById(R.id.cardView5).asInstanceOf[ImageView].setImageResource(android.R.color.transparent)
    findViewById(R.id.cardView6).asInstanceOf[ImageView].setImageResource(android.R.color.transparent)
    findViewById(R.id.cardView7).asInstanceOf[ImageView].setImageResource(android.R.color.transparent)

    findViewById(R.id.flop_list).asInstanceOf[LinearLayout].setBackgroundResource(R.drawable.lvbm_c)
    */
    /*
    findViewById(R.id.cardView1).asInstanceOf[ImageView].setImageResource(R.drawable.red_back)
    findViewById(R.id.cardView2).asInstanceOf[ImageView].setImageResource(R.drawable.red_back)
    findViewById(R.id.cardView3).asInstanceOf[ImageView].setImageResource(R.drawable.red_back)
    findViewById(R.id.cardView4).asInstanceOf[ImageView].setImageResource(R.drawable.red_back)
    findViewById(R.id.cardView5).asInstanceOf[ImageView].setImageResource(R.drawable.red_back)
    findViewById(R.id.cardView6).asInstanceOf[ImageView].setImageResource(R.drawable.red_back)
    findViewById(R.id.cardView7).asInstanceOf[ImageView].setImageResource(R.drawable.red_back)
    */
  }

  //06-aug-14
  def writeBigStatusLine(s:String){
  findViewById(R.id.bigStatusLine).asInstanceOf[TextView].setText(s)
  }
  //15-aug-14
  def writeTopStatusLine(): Unit ={

    var build=""
    if (VERSION == "Pro") {
      build = "PRO Build 1.1.4 (stabilo-android-release) - Algs 0.9.8-fe (bistro-nyet)" // on ["+ObjectUtils.getDeviceName()+"]"
    }
    else build = "FREE Build 1.1.4 (stabilo-android-release) - Algs 0.9.8-fe (bistro-nyet)"


    findViewById(R.id.builds).asInstanceOf[TextView].setText(build)

  }



  def initialFreezeViews: Unit ={

    val layout_chart = findViewById(R.id.chart).asInstanceOf[LinearLayout]
    val layout_rchart = findViewById(R.id.rchart).asInstanceOf[FrameLayout]
    val layout_mypos= findViewById(R.id.mypos).asInstanceOf[FrameLayout]
    val layout_vlistv= findViewById(R.id.vListView).asInstanceOf[ListView]
    val layout_sv= findViewById(R.id.status_view).asInstanceOf[LinearLayout]
    val layout_fl= findViewById(R.id.flop_list).asInstanceOf[TableRow]



    //val metrics:DisplayMetrics = getResources().getDisplayMetrics()

    val cObj:Configuration = this.getResources().getConfiguration()
    val sSW= cObj.smallestScreenWidthDp
    val sW = cObj.screenWidthDp
    val sH = cObj.screenHeightDp
    val sL = cObj.screenLayout

    //Log.d(TAG, "sSW ="+sSW+" sW/H="+sW+"/"+sH+" sL="+sL)

    //Amazon KFOT   sSW =600 sW/H=1024/600
    //Asus N7 G2    sSW =600 sW/H=960/527
    //Archos 97     sSW =768 sW/H=1024/720

    if (sSW>= 600 & sSW<720) { //7"
    //Log.d(TAG,"This is a 7\" Tablet")
      layout_chart.setBackgroundResource(R.drawable.r_1920_1104_2_prefloprange) //preflop bar graph
      layout_rchart.setBackgroundResource(R.drawable.r_1920_1104_2_villainbarrange) //hero/villain range bar graph
      layout_fl.setBackgroundResource(R.drawable.r_1024_720_1_cardslistmini) //mini deck blank
      layout_mypos.setBackgroundResource(R.drawable.r_1920_1104_2_equity) //monte carlo equity
      layout_vlistv.setBackgroundResource(R.drawable.r_1920_1104_2_villainouts) //villain outs
      layout_sv.setBackgroundResource(R.drawable.r_1920_1104_2_herorange) //hero range mini

      /*
      layout_chart.setBackgroundResource(R.drawable.r_1024_600_1_prefloprange) //preflop bar graph
      layout_rchart.setBackgroundResource(R.drawable.r_1024_600_1_villainbarrange) //hero/villain range bar graph
      layout_fl.setBackgroundResource(R.drawable.r_1024_720_1_cardslistmini) //mini deck blank
      layout_mypos.setBackgroundResource(R.drawable.r_1024_600_1_equity) //monte carlo equity
      layout_vlistv.setBackgroundResource(R.drawable.r_1024_600_1_villainouts) //villain outs
      layout_sv.setBackgroundResource(R.drawable.r_1024_600_1_herorange) //hero range mini
      */

    }
    else if (sSW>=720) { //10"
    //Log.d(TAG,"This is a 10\" Tablet")
      layout_chart.setBackgroundResource(R.drawable.r_1024_720_1_prefloprange) //preflop bar graph
      layout_rchart.setBackgroundResource(R.drawable.r_1024_720_1_villainbarrange) //hero/villain range bar graph
      layout_fl.setBackgroundResource(R.drawable.r_1024_720_1_cardslistmini) //mini deck blank
      layout_mypos.setBackgroundResource(R.drawable.r_1024_720_1_equity) //monte carlo equity
      layout_vlistv.setBackgroundResource(R.drawable.r_1024_720_1_villainouts) //villain outs
      layout_sv.setBackgroundResource(R.drawable.r_1024_720_1_herorange) //hero range mini

    }
    else //fallback
    {
    //Log.d(TAG,"This is an unknown Tablet of "+sW+"/"+sH)


    }

    //layout_chart.setAlpha(0.4f)
    layout_rchart.setAlpha(0.4f)
    layout_fl.setAlpha(0.4f)
    layout_mypos.setAlpha(0.4f)
    layout_vlistv.setAlpha(0.4f)
    layout_sv.setAlpha(0.4f)

  }




  /**
   * Created by ed on 7/29/14.
   * Inner Class
   */

  class DoOffUI extends AsyncTask[AnyRef, Void, Void] {
    override protected def doInBackground(ctx: AnyRef*): Void = {
      try {

        val rto:rot47 = new rot47

        //G6C:=JcE#:e52A@zei4
        //,0x47 first char

        //Log.d(TAG,"into gC..")
        ek = Character.toString(zulu71.asInstanceOf[Char])+gC("main.xml").reverse+Character.toString(zulu52.asInstanceOf[Char])
        //Log.d(TAG,"ek='"+ek+"'")


        if (ek.length!=19) {
          //corrupt initial padding fallback issue small possibility on some devices
          var fbc:Int=0
          while (ek.length !=19) {fbc+=1; ek = Character.toString(zulu71.asInstanceOf[Char])+gC("main.xml").reverse+Character.toString(zulu52.asInstanceOf[Char])}
          //Log.wtf(TAG,"FallBack "+fbc+" ek='"+ek+"'")
        }

          cry.decrypt(ctx(0).asInstanceOf[Context], R.raw.mash, rto.decode(ek)) //cac
          cryma3.decrypt(ctx(0).asInstanceOf[Context], R.raw.baz, rto.decode(ek)) //3
          cryma6.decrypt(ctx(0).asInstanceOf[Context], R.raw.las6y, rto.decode(ek)) //6
          cryma10.decrypt(ctx(0).asInstanceOf[Context], R.raw.las1y0, rto.decode(ek)) //10

      }
      catch {
        case e: Exception => {
          e.printStackTrace
        }
      }
      null
    }
  }



  //broadcast rx for data from service, override and place as inner class
  def equityBroadcastReceiver:BroadcastReceiver  = new BroadcastReceiver() {

    override def onReceive(context:Context , intent:Intent) {
      //Log.d(TAG, "br rx onReceive()")

        val ehero = intent.getStringExtra("ehero")
        val evillain = intent.getStringExtra("evillain")
        val emid = intent.getStringExtra("emid")

        val hperc: TextView = findViewById(R.id.hmid).asInstanceOf[TextView]
        val eh: TextView = findViewById(R.id.ehero).asInstanceOf[TextView]
        val ev: TextView = findViewById(R.id.evillain).asInstanceOf[TextView]

        eh.setText(ehero+"%") //vlo hands
        hperc.setText(emid+"%")
        ev.setText(evillain+"%") //vhi hands


      }

    }

  //these are async callbacks required by the binder service once we bind to our service in the onCreate() of activity

  /** Defines callbacks for service binding, passed to bindService() */
  def mConnection:ServiceConnection = new ServiceConnection {

    override def onServiceConnected(name: ComponentName, service: IBinder): Unit = {
      mBound+=1
      //Log.d(TAG,"a binder onServiceConnected ")
      // We've bound to LocalService, cast the IBinder and get LocalService instance
      val binder = service.asInstanceOf[EquityBroadcastServiceWrapper#LocalBinder] //http://www.scala-lang.org/old/node/6651.html for the '#' answer!!!
      mService = binder.getService()


    }

    override def onServiceDisconnected(name: ComponentName): Unit = {
      mBound-=1
      //Log.d(TAG,"a binder onServiceDisconnected ")
    }
  }




} //PrimaryActivity

