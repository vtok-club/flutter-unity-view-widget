package com.xraph.plugin.flutter_unity_widget

import android.app.Activity
import android.util.Log
import android.view.WindowManager
import android.widget.FrameLayout
import com.unity3d.player.IUnityPlayerLifecycleEvents
import com.unity3d.player.UnityPlayer
import java.util.concurrent.CopyOnWriteArraySet


class UnityPlayerUtils {

    companion object {
        private const val LOG_TAG = "UnityPlayerUtils"

        var controllers: ArrayList<FlutterUnityWidgetController> = ArrayList()
        var unityPlayer: CustomUnityPlayer? = null
        var activity: Activity? = null
        var prevActivityRequestedOrientation: Int? = null

        var options: FlutterUnityWidgetOptions = FlutterUnityWidgetOptions()

        var unityPaused: Boolean = false
        var unityLoaded: Boolean = false
        var viewStaggered: Boolean = false

        private val mUnityEventListeners = CopyOnWriteArraySet<UnityEventListener>()

        fun focus() {
            try {
                unityPlayer!!.windowFocusChanged(unityPlayer!!.requestFocus())
                unityPlayer!!.resume()
            } catch (e: Exception) {
                Log.e(LOG_TAG, e.toString())
            }
        }

        /**
         * Create a new unity player with callback
         */
        fun createUnityPlayer(ule: IUnityPlayerLifecycleEvents, callback: OnCreateUnityViewCallback?) {
            if (activity == null) {
                throw java.lang.Exception("Unity activity is null")
            }

            if (unityPlayer != null) {
                unityLoaded = true
                focus()
                callback?.onReady()
                return
            }

            try {
                unityPlayer = CustomUnityPlayer(activity!!, ule)
                unityLoaded = true

                if (!options.fullscreenEnabled) {
                    activity!!.window.addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
                    activity!!.window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                } else {
                    activity!!.window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                }

                focus()
                callback?.onReady()
            } catch (e: Exception) {
                Log.e(LOG_TAG, e.toString())
            }
        }

        fun postMessage(gameObject: String, methodName: String, message: String) {
            UnityPlayer.UnitySendMessage(gameObject, methodName, message)
        }

        fun pause() {
            try {
                if (unityPlayer != null) {
                    unityPlayer!!.pause()
                    unityPaused = true
                }
            } catch (e: Exception) {
                Log.e(LOG_TAG, e.toString())
            }
        }

        fun resume() {
            try {
                if (unityPlayer != null) {
                    unityPlayer!!.resume()
                    unityPaused = false
                }
            } catch (e: Exception) {
                Log.e(LOG_TAG, e.toString())
            }
        }

        fun unload() {
            try {
                if (unityPlayer != null) {
                    unityPlayer!!.unload()
                    unityLoaded = false
                }
            } catch (e: Exception) {
                Log.e(LOG_TAG, e.toString())
            }
        }

        fun quitPlayer() {
            try {
                if (unityPlayer != null) {
                    unityPlayer!!.quit()
                    unityLoaded = false
                }
            } catch (e: Error) {
                e.message?.let { Log.e(LOG_TAG, it) }
            }
        }

        /**
         * Invoke by unity C#
         */
        @JvmStatic
        fun onUnitySceneLoaded(name: String, buildIndex: Int, isLoaded: Boolean, isValid: Boolean) {
            for (listener in mUnityEventListeners) {
                try {
                    listener.onSceneLoaded(name, buildIndex, isLoaded, isValid)
                } catch (e: Exception) {
                    e.message?.let { Log.e(LOG_TAG, it) }
                }
            }
        }

        /**
         * Invoke by unity C#
         */
        @JvmStatic
        fun onUnityMessage(message: String) {
            Log.d("UnityListener", "total listeners are ${mUnityEventListeners.size}")
            for (listener in mUnityEventListeners) {
                try {
                    listener.onMessage(message)
                } catch (e: Exception) {
                    e.message?.let { Log.e(LOG_TAG, it) }
                }
            }
        }

        fun addUnityEventListener(listener: UnityEventListener) {
            mUnityEventListeners.add(listener)
        }

        fun removeUnityEventListener(listener: UnityEventListener) {
            mUnityEventListeners.remove(listener)
        }

        private fun shakeActivity() {
            unityPlayer?.windowFocusChanged(true)
            if (prevActivityRequestedOrientation != null) {
                activity?.requestedOrientation = prevActivityRequestedOrientation!!
            }
        }

        fun removePlayer(controller: FlutterUnityWidgetController) {
            if (unityPlayer!!.parent == controller.view) {
                if (controllers.isEmpty()) {
                    (controller.view as FrameLayout).removeView(unityPlayer)
                    pause()
                    shakeActivity()
                } else {
                    controllers[controllers.size - 1].reattachToView()
                }
            }
        }

        fun reset() {
            unityLoaded = false
        }
    }
}