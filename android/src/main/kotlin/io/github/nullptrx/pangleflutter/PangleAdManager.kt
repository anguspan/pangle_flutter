package io.github.nullptrx.pangleflutter

import android.app.Activity
import android.content.Context
import android.content.pm.PackageInfo
import com.bytedance.sdk.openadsdk.*
import io.flutter.plugin.common.MethodChannel
import io.github.nullptrx.pangleflutter.common.PangleTitleBarTheme
import io.github.nullptrx.pangleflutter.delegate.FLTFeedAd
import io.github.nullptrx.pangleflutter.delegate.FLTFeedExpressAd
import io.github.nullptrx.pangleflutter.delegate.FLTRewardedVideoAd
import io.github.nullptrx.pangleflutter.delegate.RewardAdInteractionImpl
import io.github.nullptrx.pangleflutter.util.asMap
import java.util.*


class PangleAdManager {

  companion object {
    val shared = PangleAdManager()
  }


  private val feedAdCollection = Collections.synchronizedMap<String, TTFeedAd>(mutableMapOf<String, TTFeedAd>())
  private val bannerAdCollection = Collections.synchronizedMap<String, TTBannerAd>(mutableMapOf<String, TTBannerAd>())
  private val expressAdCollection = Collections.synchronizedMap<String, TTNativeExpressAd>(mutableMapOf<String, TTNativeExpressAd>())
  private val rewardedVideoAdCollection = Collections.synchronizedList<TTRewardVideoAd>(mutableListOf<TTRewardVideoAd>())

  private var ttAdManager: TTAdManager? = null
  private var ttAdNative: TTAdNative? = null
    get() = field


  /**
   * Feed
   */
  fun setFeedAd(ttFeedAds: List<TTFeedAd>): List<String> {
    val data = mutableListOf<String>()
    ttFeedAds.forEach {
      val key = it.hashCode().toString()
      feedAdCollection[key] = it
      data.add(key)
    }
    return data
  }

  fun getFeedAd(key: String): TTFeedAd? {
    return feedAdCollection[key]
  }

  fun removeFeedAd(key: String) {
    feedAdCollection.remove(key)
  }

  /**
   * Banner
   */
  fun setBannerAd(ttBannerAds: List<TTBannerAd>): List<String> {
    val data = mutableListOf<String>()
    ttBannerAds.forEach {
      val key = it.hashCode().toString()
      bannerAdCollection[key] = it
      data.add(key)
    }
    return data
  }

  fun getBannerAd(key: String): TTBannerAd? {
    return bannerAdCollection[key]
  }

  fun removeBannerAd(key: String) {
    bannerAdCollection.remove(key)
  }

  /**
   * Express
   */
  fun setExpressAd(ttBannerAds: List<TTNativeExpressAd>): List<String> {
    val data = mutableListOf<String>()
    ttBannerAds.forEach {
      // 预先渲染
      it.render()
      val key = it.hashCode().toString()
      expressAdCollection[key] = it
      data.add(key)
    }
    return data
  }

  fun getExpressAd(key: String): TTNativeExpressAd? {
    return expressAdCollection[key]
  }

  fun removeExpressAd(key: String) {
    val it = expressAdCollection.remove(key)
    it?.destroy()
  }

  fun showRewardedVideoAd(result: MethodChannel.Result, activity: Activity?): Boolean {
    activity ?: return false
    if (rewardedVideoAdCollection.size > 0) {
      val ad = rewardedVideoAdCollection.removeAt(0)
      ad.setRewardAdInteractionListener(RewardAdInteractionImpl { obj ->
        result.success(obj)
      })
      ad.showRewardVideoAd(activity)
      return true
    }
    return false
  }

  fun setRewardedVideoAd(ad: TTRewardVideoAd?) {
    ad?.also {
      rewardedVideoAdCollection.add(it)
    }
  }


  fun initialize(activity: Activity?, args: Map<String, Any?>) {
    activity ?: return
    val context: Context = activity

    val appId = args["appId"] as String
    val debug = args["debug"] as Boolean?
    val allowShowNotify = args["allowShowNotify"] as Boolean?
    val allowShowPageWhenScreenLock = args["allowShowPageWhenScreenLock"] as Boolean?
    val supportMultiProcess = args["supportMultiProcess"] as Boolean?
    val useTextureView = args["useTextureView"] as Boolean?
    val directDownloadNetworkType = args["directDownloadNetworkType"] as Int?
    val paid = args["paid"] as Boolean?
    val titleBarThemeIndex = args["titleBarTheme"] as Int?
    val isCanUseLocation = args["isCanUseLocation"] as Boolean?
    val isCanUsePhoneState = args["isCanUsePhoneState"] as Boolean?
    val isCanUseWriteExternal = args["isCanUseWriteExternal"] as Boolean?
    val isCanUseWifiState = args["isCanUseWifiState"] as Boolean?
    val devImei = args["devImei"] as String?
    val devOaid = args["devOaid"] as String?
    val location = args["location"]?.asMap<String, Double>()
    var ttLocation: TTLocation? = null
    location?.also {
      try {
        val latitude = it["latitude"]!!
        val longitude = it["longitude"]!!
        ttLocation = TTLocation(latitude, longitude)
      } catch (e: Exception) {
      }
    }

    var titleBarTheme: Int? = null
    if (titleBarThemeIndex != null) {
      titleBarTheme = PangleTitleBarTheme.values()[titleBarThemeIndex].value
    }

    //强烈建议在应用对应的Application#onCreate()方法中调用，避免出现content为null的异常
    val packageManager = context.packageManager
    val applicationContext = context.applicationContext
    val pkgInfo: PackageInfo = packageManager.getPackageInfo(applicationContext.packageName, 0)
    //获取应用名
    val appName = pkgInfo.applicationInfo.loadLabel(packageManager).toString()

    val config = TTAdConfig.Builder().apply {
      appName(appName)
      appId(appId)
      debug?.also {
        debug(it)
      }
      useTextureView?.also {
        useTextureView(it)
      }

      titleBarTheme?.also {
        titleBarTheme(it)
      }
      titleBarTheme(TTAdConstant.TITLE_BAR_THEME_LIGHT)

      allowShowNotify?.also {
        allowShowNotify(it)
      }
      allowShowPageWhenScreenLock?.also {
        allowShowPageWhenScreenLock(it)
      }
      directDownloadNetworkType?.also {
        directDownloadNetworkType(it)
      }
      supportMultiProcess?.also {
        supportMultiProcess(it)
      }
      paid?.also {
        paid(it)
      }

      customController(object : TTCustomController() {
        override fun isCanUseLocation(): Boolean {

          return isCanUseLocation ?: super.isCanUseLocation()
        }

        override fun isCanUsePhoneState(): Boolean {
          return isCanUsePhoneState ?: super.isCanUsePhoneState()
        }

        override fun isCanUseWriteExternal(): Boolean {
          return isCanUseWriteExternal ?: super.isCanUseWriteExternal()
        }

        override fun isCanUseWifiState(): Boolean {
          return isCanUseWifiState ?: super.isCanUseWifiState()
        }

        override fun getDevImei(): String {
          return devImei ?: super.getDevImei()
        }

        override fun getTTLocation(): TTLocation {
          return ttLocation ?: super.getTTLocation()
        }

        override fun getDevOaid(): String {
          return devOaid ?: super.getDevOaid()
        }
      })

    }.build()

    TTAdSdk.init(applicationContext, config)

    ttAdManager = TTAdSdk.getAdManager()
    ttAdNative = ttAdManager?.createAdNative(activity)

  }

  fun requestPermissionIfNecessary(context: Context) {
    ttAdManager?.requestPermissionIfNecessary(context)
  }

  fun loadSplashAd(adSlot: AdSlot, listener: TTAdNative.SplashAdListener, timeout: Float? = null) {
    if (timeout == null) {
      ttAdNative?.loadSplashAd(adSlot, listener)
    } else {
      ttAdNative?.loadSplashAd(adSlot, listener, (timeout * 1000).toInt())
    }

  }

  fun loadRewardVideoAd(adSlot: AdSlot, result: MethodChannel.Result, activity: Activity?, preload: Boolean) {

    activity ?: return

    ttAdNative?.loadRewardVideoAd(adSlot, FLTRewardedVideoAd({ obj ->
      result.success(obj)
    }, activity, preload))

  }

  fun loadFeedAd(adSlot: AdSlot, result: MethodChannel.Result) {
    ttAdNative?.loadFeedAd(adSlot, FLTFeedAd(result))
  }

  fun loadFeedExpressAd(adSlot: AdSlot, result: MethodChannel.Result) {
    ttAdNative?.loadNativeExpressAd(adSlot, FLTFeedExpressAd(result))
  }

  fun loadBannerAd(adSlot: AdSlot, listener: TTAdNative.BannerAdListener) {
    ttAdNative?.loadBannerAd(adSlot, listener)
  }

  fun loadBannerExpressAd(adSlot: AdSlot, listener: TTAdNative.NativeExpressAdListener) {
    ttAdNative?.loadBannerExpressAd(adSlot, listener)
  }

  fun loadInteractionAd(adSlot: AdSlot, listener: TTAdNative.InteractionAdListener) {
    ttAdNative?.loadInteractionAd(adSlot, listener)
  }

  fun loadInteractionExpressAd(adSlot: AdSlot, listener: TTAdNative.NativeExpressAdListener) {
    ttAdNative?.loadInteractionExpressAd(adSlot, listener)
  }


}
