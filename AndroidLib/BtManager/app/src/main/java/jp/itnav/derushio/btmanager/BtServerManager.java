package jp.itnav.derushio.btmanager;

import android.bluetooth.BluetoothServerSocket;
import android.content.Context;
import android.os.Message;

import java.io.IOException;

/**
 * Created by derushio on 15/02/27.
 * Bluetoothサーバーを行うための主要処理をまとめたクラス
 * これを直接インスタンスして使っても良い。
 */
public class BtServerManager extends BtManagerBase {
	protected BluetoothServerSocket mBtServerSocket;
	// Bluetooth通信をホストするためのソケット

	private String mBtServerName;
	// 設定されたサーバーの名前

	public BtServerManager(Context context, int messageMailBoxLength) {
		super(context, messageMailBoxLength);
	}
	// コンストラクタ

	public void startBtServer(final String btServerName) {
		final Message message = new Message();

		if (isDeviceConnected()) {
			message.what = 1;
			mOnConnectListener.sendMessage(message);
			return;
			// すでに接続している
		}


		Thread serverStart = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					mBtServerSocket = mBtAdapter.listenUsingInsecureRfcommWithServiceRecord(btServerName, SPP_UUID);
					// BluetoothサーバーをSPPモードで起動するように設定
				} catch (IOException e) {
					// IOExceptionをキャッチした
					e.printStackTrace();
					// エラーメッセージを吐く

					message.what = -1;
					mOnConnectListener.sendMessage(message);
					// エラーｰ1を送る(ソケットが見つからない)
					return;
					// 設定を作れなかったのでこれ以上実行する必要がない
				}

				if (mBtAdapter.isDiscovering()) {
					mBtAdapter.cancelDiscovery();
					// 探索中だったらキャンセルする
				}

				try {
					mBtSocket = mBtServerSocket.accept();
					// Bluetoothサーバーを開始

					//できたらこれ以下の処理が走る
					message.what = 0;
					mOnConnectListener.sendMessage(message);
					// 完了0を送る(接続成功)
					mBtServerName = btServerName;
				} catch (IOException e) {
					// IOExceptionをキャッチ(接続できない)
					e.printStackTrace();
					message.what = -2;
					mOnConnectListener.sendMessage(message);
					// エラー-2を送る(デバイスが見つからない)
					return;
					// 接続できなかったのでこれ以上する必要はない
				}

				try {
					mBtInputStream = mBtSocket.getInputStream();
					mBtOutputStream = mBtSocket.getOutputStream();
				} catch (IOException e) {
					e.printStackTrace();
					// エラーを吐く
				}
			}
		});

		serverStart.start();
	}
	// Bluetooth通信をホストする

	public void stopBtServer(final boolean restart) {
		final Message message = new Message();

		if (isBtSocketExists()) {
			Thread stopServer = new Thread(new Runnable() {
				@Override
				public void run() {
					// ここからは時間がかかる処理なので、非同期処理で行う
					try {
						mBtSocket.close();
						mBtServerSocket.close();
						// 切断する

						//できたらこれ以下の処理が走る
						message.what = 0;
						mOnDisconnectListener.sendMessage(message);
						// 切断成功
					} catch (IOException e) {
						message.what = -1;
						mOnDisconnectListener.sendMessage(message);
						e.printStackTrace();
						// 切断失敗
					}

					if (restart) {
						startBtServer(mBtServerName);
						// 接続し直す
					}
				}
			});

			stopServer.start();
			// 非同期処理開始
		} else {
			message.what = -2;
			mOnDisconnectListener.sendMessage(message);
			// デバイスに接続されていないエラーを投げる
			if (restart) {
				startBtServer(mBtServerName);
				// 接続し直す
			}
		}
	}
	// Bluetooth通信ホストを停止する

	public void stopBtServer() {
		stopBtServer(false);
	}
	// Bluetooth通信ホストを停止する(オーバーライド)

	public void restartBtServer() {
		stopBtServer(true);
	}
	// Bluetooth通信ホストを再スタートする

	public String getBtServerName() {
		return mBtServerName;
	}
	// サーバーの名前を取得
}

/**
 * だんだん疲れてきた
 */
