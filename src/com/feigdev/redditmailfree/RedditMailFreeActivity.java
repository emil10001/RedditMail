package com.feigdev.redditmailfree;

import java.util.Date;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Messenger;
import android.text.Html;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;
import android.widget.TextView;
import android.widget.Toast;

import com.feigdev.redditmailcommon.Constants;
import com.feigdev.redditmailcommon.RedditFriends;
import com.feigdev.redditmailcommon.RedditMailListener;
import com.feigdev.redditmailcommon.RedditMailService;
import com.feigdev.redditmailcommon.RedditMessages;
import com.feigdev.redditmailpro.R;
import com.feigdev.redditmailpro.example.android.actionbarcompat.ActionBarActivity;

public class RedditMailFreeActivity extends ActionBarActivity implements
		RedditMailListener {
	private static final String TAG = "RedditMailFreeActivity";
	public static boolean isRunning = false;
	private RedditMailService mBoundService;
	Messenger mService = null;
	private ListAdapter listAdapter;
	private static final int INBOX = 0;
	private static final int SENT = 1;
	private static final int FRIEND_MESSAGES = 2;
	private static final int FRIENDS = 3;
	private ProgressDialog pDialog;
	private RedditMessages messageQueue;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		startService(new Intent(RedditMailFreeActivity.this,
				RedditMailService.class));
		doBindService();
		setContentView(R.layout.login);

		findViewById(R.id.progressBar1).setVisibility(View.INVISIBLE);

		findViewById(R.id.login_button).setOnClickListener(
				new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						if (mBoundService != null) {
							findViewById(R.id.progressBar1).setVisibility(
									View.VISIBLE);
							mBoundService
									.requestLogin(
											((EditText) findViewById(R.id.username_text))
													.getText().toString(),
											((EditText) findViewById(R.id.password_text))
													.getText().toString());
						} else {
							if (Constants.DEBUG) {
								Log.d(TAG, "login clicked - service not bound");
							}
						}
					}
				});

		findViewById(R.id.cancel_button).setOnClickListener(
				new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						finish();
					}
				});
		listAdapter = new ListAdapter();
		messageQueue = new RedditMessages();

	}

	@Override
	public void onStart() {
		super.onStart();
	}

	@Override
	public void onResume() {
		super.onResume();
		NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		nm.cancel(R.string.notify_message);
		nm.cancelAll();
		isRunning = true;
	}

	@Override
	public void onPause() {
		super.onPause();
		isRunning = false;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater menuInflater = getMenuInflater();
		menuInflater.inflate(R.menu.main, menu);

		// Calling super after populating the menu is necessary here to ensure
		// that the
		// action bar helpers have a chance to handle this event.
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		if (mBoundService != null) {
			if (Constants.DEBUG) {
				Log.i(TAG, "Service connected");
			}
			if (!mBoundService.isLoggedIn()) {
				menu.getItem(0).setEnabled(false);
				menu.getItem(0).setVisible(false);
				menu.getItem(1).setVisible(false);
				menu.getItem(1).setEnabled(false);
				menu.getItem(2).setVisible(false);
				menu.getItem(2).setEnabled(false);

			} else {
				menu.getItem(0).setEnabled(true);
				menu.getItem(0).setVisible(true);
				menu.getItem(1).setVisible(true);
				menu.getItem(1).setEnabled(true);
				menu.getItem(2).setVisible(true);
				menu.getItem(2).setEnabled(true);
			}
		} else {
			menu.getItem(0).setEnabled(false);
			menu.getItem(0).setVisible(false);
			menu.getItem(1).setVisible(false);
			menu.getItem(1).setEnabled(false);
			menu.getItem(2).setVisible(false);
			menu.getItem(2).setEnabled(false);
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		// case android.R.id.home:
		// if (Constants.DEBUG){ Toast.makeText(this, "Tapped home",
		// Toast.LENGTH_SHORT).show();}
		// break;

		case R.id.menu_refresh:
			if (mBoundService != null) {
				if (mBoundService.isLoggedIn()) {
					mBoundService.refreshAll();
				} else {
					Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT)
							.show();
				}
			} else {
				webErrorDialog("Error", "Ok", "Exit", "Service not connected");
			}
			getActionBarHelper().setRefreshActionItemState(true);
			getWindow().getDecorView().postDelayed(new Runnable() {
				@Override
				public void run() {
					getActionBarHelper().setRefreshActionItemState(false);
				}
			}, 1000);
			break;
		case R.id.menu_compose:
			messageQueue = null;
			messageQueue = new RedditMessages();
			buildComposeNewDialog();
			break;
		case R.id.menu_logout:
			if (mBoundService != null) {
				mBoundService.logout();
			}
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onDestroy() {
		if (Constants.DEBUG) {
			Log.i(TAG, "onDestroy called");
		}
		doUnbindService();
		super.onDestroy();
	}

	public void webErrorDialog(String title, String positive, String negative,
			String message) {
		final AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(title);
		builder.setPositiveButton(positive,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {

					}
				});
		if (negative.equals("") || negative == null) {
			builder.setCancelable(false);
		} else {
			builder.setCancelable(true);
			builder.setNegativeButton(negative,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							finish();
						}
					});
		}

		builder.setMessage(message);
		builder.create().show();
	}

	public void sendReply() {
		if (messageQueue.getContent().equals("")) {
			this.onPostFailure("Empty message, did not send.");
			return;
		}
		if (messageQueue.getId().equals("")) {
			mBoundService.postPM(messageQueue);
			messageQueue = new RedditMessages();
			return;
		}
		mBoundService.postMessages(messageQueue);
		messageQueue = new RedditMessages();
	}

	public void buildEnterTextDialog() {
		final Dialog dialog = new Dialog(this);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			dialog.setContentView(R.layout.enter_text);
		} else {
			WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
			lp.copyFrom(dialog.getWindow().getAttributes());
			lp.horizontalMargin = 10;
			lp.width = WindowManager.LayoutParams.FILL_PARENT;
			lp.height = WindowManager.LayoutParams.WRAP_CONTENT;

			dialog.setContentView(R.layout.enter_text);
			dialog.getWindow().setAttributes(lp);
		}

		String titleString = "To: " + messageQueue.getRecipient();
		if (Constants.DEBUG) {
			Log.d(TAG, titleString);
		}
		dialog.setTitle(titleString);
		final EditText et = (EditText) dialog
				.findViewById(R.id.enter_text_content);
		et.setText(messageQueue.getContent());

		dialog.findViewById(R.id.send_button).setOnClickListener(
				new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						messageQueue.setContent(et.getText().toString());
						sendReply();
						dialog.cancel();
					}
				});
		dialog.findViewById(R.id.clear_button).setOnClickListener(
				new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						messageQueue.setContent("");
						et.setText(messageQueue.getContent());
					}
				});
		dialog.findViewById(R.id.cancel_button).setOnClickListener(
				new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						messageQueue.setContent(et.getText().toString());
						dialog.cancel();
					}
				});
		dialog.show();
	}

	public void buildComposeNewDialog() {
		final Dialog dialog = new Dialog(this);
		String friendName;
		String subject;

		if (messageQueue.getRecipient().equals("")) {
			friendName = "recipient";
		} else {
			friendName = messageQueue.getRecipient();
		}
		if (messageQueue.getSubject().equals("")) {
			subject = "subject";
		} else {
			subject = messageQueue.getSubject();
		}

		messageQueue = null;
		messageQueue = new RedditMessages();

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			dialog.setContentView(R.layout.generic_enter_text);
		} else {
			WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
			lp.copyFrom(dialog.getWindow().getAttributes());
			lp.horizontalMargin = 10;
			lp.width = WindowManager.LayoutParams.FILL_PARENT;
			lp.height = WindowManager.LayoutParams.WRAP_CONTENT;

			dialog.setContentView(R.layout.generic_enter_text);
			dialog.getWindow().setAttributes(lp);
		}

		dialog.setTitle(R.string.compose_new_title);
		final EditText et = (EditText) dialog
				.findViewById(R.id.genter_text_content);
		final EditText recipient = (EditText) dialog
				.findViewById(R.id.recipient);
		final EditText subj = (EditText) dialog.findViewById(R.id.subject);

		et.setText("");
		recipient.setText(friendName);
		subj.setText(subject);

		dialog.findViewById(R.id.gsend_button).setOnClickListener(
				new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						messageQueue.setContent(et.getText().toString());
						messageQueue.setRecipient(recipient.getText()
								.toString());
						messageQueue.setSubject(subj.getText().toString());
						sendReply();
						dialog.cancel();
					}
				});
		dialog.findViewById(R.id.gclear_button).setOnClickListener(
				new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						messageQueue.setContent("");
						et.setText(messageQueue.getContent());
					}
				});
		dialog.findViewById(R.id.gcancel_button).setOnClickListener(
				new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						messageQueue.setContent(et.getText().toString());
						messageQueue.setRecipient(recipient.getText()
								.toString());
						messageQueue.setSubject(subj.getText().toString());
						dialog.cancel();
					}
				});
		dialog.show();
	}

	/***
	 * Initiates a connection to the RedditMailService
	 * 
	 * registers the local handler with the service
	 * 
	 * registers the remote handler with this class
	 * 
	 */
	private ServiceConnection mConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			if (Constants.DEBUG) {
				Log.i(TAG, "onServiceConnected called");
			}
			mBoundService = ((RedditMailService.LocalBinder) service)
					.getService();
			mBoundService.cancelNotify();
			mBoundService.setRedditMailListener(RedditMailFreeActivity.this);
			if (mBoundService != null) {
				if (Constants.DEBUG) {
					Log.i(TAG, "Service connected");
				}
				if (mBoundService.isLoggedIn()) {
					handler.post(new Runnable() {
						public void run() {
							login();
						}
					});
				}
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName className) {
			mBoundService = null;
			if (Constants.DEBUG) {
				Log.i(TAG, "Service disconnected");
			}
		}
	};

	void doBindService() {
		if (Constants.DEBUG) {
			Log.i(TAG, "doBindService called");
		}
		Intent intent = new Intent(RedditMailFreeActivity.this,
				RedditMailService.class);
		if (!getApplicationContext().bindService(intent, mConnection,
				Context.BIND_AUTO_CREATE)) {
			if (Constants.DEBUG) {
				Log.i(TAG, "doBindService failed");
			}
			throw new IllegalStateException("binding to service failed"
					+ intent);
		} else {
			if (Constants.DEBUG) {
				Log.i(TAG, "doBindService succeeded");
			}
		}
	}

	void doUnbindService() {
		if (mBoundService != null) {
			getApplicationContext().unbindService(mConnection);
			if (Constants.DEBUG) {
				Log.i(TAG, "Unbinding");
			}
		}
	}

	private void login() {
		findViewById(R.id.progressBar1).setVisibility(View.INVISIBLE);
		setContentView(R.layout.main);

		TabHost tabHost = (TabHost) findViewById(R.id.tabHost);
		tabHost.setup();

		TabSpec spec1 = tabHost.newTabSpec("inbox");
		spec1.setContent(R.id.inbox_view);

		TabSpec spec2 = tabHost.newTabSpec("sent");
		spec2.setContent(R.id.sent_view);

		TabSpec spec3 = tabHost.newTabSpec("friends");

		spec3.setContent(R.id.friend_view);

		TabSpec spec4 = tabHost.newTabSpec("friends comments");

		spec4.setContent(R.id.friends_comments_view);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			spec1.setIndicator("inbox");
			spec2.setIndicator("sent");
			spec3.setIndicator("friends");
			spec4.setIndicator("friends comments");

		} else {
			TextView tv1 = new TextView(this);
			tv1.setText("INBOX");
			tv1.setGravity(Gravity.CENTER);
			spec1.setIndicator(tv1);
			TextView tv2 = new TextView(this);
			tv2.setText("SENT");
			tv2.setGravity(Gravity.CENTER);
			spec2.setIndicator(tv2);
			TextView tv3 = new TextView(this);
			tv3.setText("FRIENDS");
			tv3.setGravity(Gravity.CENTER);
			spec3.setIndicator(tv3);
			TextView tv4 = new TextView(this);
			tv4.setText("FRIENDS COMMENTS");
			tv4.setGravity(Gravity.CENTER);
			spec4.setIndicator(tv4);

		}

		tabHost.addTab(spec1);
		tabHost.addTab(spec2);
		tabHost.addTab(spec3);
		tabHost.addTab(spec4);
		if (Constants.DEBUG) {
			Log.d(TAG, "refreshMenu() called");
		}
		pDialog = ProgressDialog.show(RedditMailFreeActivity.this, "",
				"Loading. Please wait...", true);
		refreshMenu();
		requestCurrentData();
	}

	public void requestCurrentData() {
		if (mBoundService != null) {
			mBoundService.refreshAll();
			if (mBoundService.getMessages() != null) {
				if (mBoundService.getMessages().size() > 0) {
					onInboxUpdate();
				}
			}
			if (mBoundService.getSentMessages() != null) {
				if (mBoundService.getSentMessages().size() > 0) {
					onSentUpdate();
				}
			}
			if (mBoundService.getFriends() != null) {
				if (mBoundService.getFriends().size() > 0) {
					onFriendsUpdate();
				}
			}
			if (mBoundService.getFriendsMessages() != null) {
				if (mBoundService.getFriendsMessages().size() > 0) {
					onFriendsMessages();
				}
			}
		}
	}

	private void toastText(String message) {
		Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT)
				.show();
	}

	private void messageMenu(final int type) {
		try {
			final CharSequence[] messageItems = { "Send message",
					"Open in browser", "Back" };
			final CharSequence[] friendItems = { "Send message",
					"Open in browser", "Back" };
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			switch (type) {
			case FRIENDS:
				builder.setItems(friendItems,
						new DialogInterface.OnClickListener() {
							public void onClick(final DialogInterface dialog,
									int item) {
								String url;
								switch (item) {
								case 0:
									handler.post(new Runnable() {
										public void run() {
											buildComposeNewDialog();
										}
									});
									dialog.dismiss();
									break;
								case 1:
									url = "http://www.reddit.com/user/"
											+ messageQueue.getRecipient();
									startActivity(new Intent(Intent.ACTION_VIEW)
											.setData(Uri.parse(url)));
									break;
								case 2:
									dialog.dismiss();
									break;
								}

							}
						});
				builder.create().show();
				break;
			case SENT:
				builder.setItems(messageItems,
						new DialogInterface.OnClickListener() {
							public void onClick(final DialogInterface dialog,
									int item) {
								String url;
								switch (item) {
								case 0:
									handler.post(new Runnable() {
										public void run() {
											buildComposeNewDialog();
										}
									});
									dialog.dismiss();
									break;
								case 1:
									if (mBoundService.isPM(messageQueue)) {
										url = "http://www.reddit.com/message/inbox/";
									} else {
										if (!messageQueue.getContext().equals(
												"")) {
											url = "http://www.reddit.com"
													+ messageQueue.getContext();
										} else if (!messageQueue.getParent()
												.equals("")) {
											url = "http://www.reddit.com/by_id/"
													+ messageQueue.getParent();
										} else {
											url = "http://www.reddit.com/by_id/"
													+ messageQueue.getId();
										}
									}
									startActivity(new Intent(Intent.ACTION_VIEW)
											.setData(Uri.parse(url)));
									break;
								case 2:
									dialog.dismiss();
									break;
								}

							}
						});
				builder.create().show();
				break;
			default:
				builder.setItems(messageItems,
						new DialogInterface.OnClickListener() {
							public void onClick(final DialogInterface dialog,
									int item) {
								String url;
								switch (item) {
								case 0:
									handler.post(new Runnable() {
										public void run() {
											buildEnterTextDialog();
										}
									});
									dialog.dismiss();
									break;
								case 1:
									if (mBoundService.isPM(messageQueue)) {
										url = "http://www.reddit.com/message/inbox/";
									} else {
										if (!messageQueue.getContext().equals(
												"")) {
											url = "http://www.reddit.com"
													+ messageQueue.getContext();
										} else if (!messageQueue.getParent()
												.equals("")) {
											url = "http://www.reddit.com/by_id/"
													+ messageQueue.getParent();
										} else {
											url = "http://www.reddit.com/by_id/"
													+ messageQueue.getId();
										}
									}
									startActivity(new Intent(Intent.ACTION_VIEW)
											.setData(Uri.parse(url)));
									break;
								case 2:
									dialog.dismiss();
									break;
								}

							}
						});
				builder.create().show();
				break;
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private void listViewRebuild(final int typeId) {
		final String curReplyText = messageQueue.getContent();
		if (listAdapter.getAdapter(typeId) == null) {
			if (Constants.DEBUG) {
				Log.d(TAG, "listView.setAdapter() called");
			}
			switch (typeId) {
			case INBOX:
				listAdapter.setListView(
						(ListView) findViewById(R.id.inbox_view), typeId);
				break;
			case SENT:
				listAdapter.setListView(
						(ListView) findViewById(R.id.sent_view), typeId);
				break;
			case FRIEND_MESSAGES:
				listAdapter.setListView(
						(ListView) findViewById(R.id.friends_comments_view),
						typeId);
				break;
			case FRIENDS:
				listAdapter.setListView(
						(ListView) findViewById(R.id.friend_view), typeId);
				break;
			}
			listAdapter.setAdapter(new MessageAdapter(
					RedditMailFreeActivity.this, typeId), typeId);
			listAdapter.getListView(typeId).setAdapter(
					listAdapter.getAdapter(typeId));
			listAdapter.getListView(typeId).setClickable(true);
			listAdapter.getListView(typeId).setOnItemClickListener(
					new AdapterView.OnItemClickListener() {

						@Override
						public void onItemClick(AdapterView<?> arg0, View arg1,
								final int position, long arg3) {
							handler.post(new Runnable() {
								public void run() {
									if (Constants.DEBUG) {
										Log.d(TAG, "an item was clicked");
									}
									switch (typeId) {
									case INBOX:
										messageQueue = null;
										messageQueue = new RedditMessages(
												mBoundService.getMessages()
														.get(position));
										messageQueue.setRecipient(messageQueue
												.getSender());
										messageQueue.setContent(curReplyText);
										messageQueue.setParent("");
										messageMenu(INBOX);
										if (messageQueue.isNew()) {
											RedditMessages read = new RedditMessages();
											read.setId(messageQueue.getId());
											mBoundService.markRead(read);
											mBoundService.getMessages()
													.get(position)
													.setNew(false);
											mBoundService
													.addReadMessageId(mBoundService
															.getMessages()
															.get(position)
															.getId());
											listViewRebuild(INBOX);
										}
										break;
									case SENT:
										messageQueue = null;
										messageQueue = new RedditMessages(
												mBoundService.getSentMessages()
														.get(position));
										messageQueue.setRecipient(mBoundService
												.getSentMessages()
												.get(position).getRecipient());
										messageQueue.setContent(curReplyText);
										messageQueue.setParent("");
										messageMenu(SENT);
										break;
									case FRIEND_MESSAGES:
										messageQueue = null;
										messageQueue = new RedditMessages(
												mBoundService
														.getFriendsMessages()
														.get(position));
										messageQueue.setRecipient(messageQueue
												.getSender());
										messageQueue.setContent(curReplyText);
										messageMenu(FRIEND_MESSAGES);
										break;
									case FRIENDS:
										messageQueue = null;
										messageQueue = new RedditMessages();
										messageQueue.setContent(curReplyText);
										messageQueue.setRecipient(mBoundService
												.getFriends().get(position)
												.getFriend());
										messageQueue.setId("");
										messageMenu(FRIENDS);
										break;
									}
								}
							});
						}
					});
		} else {
			if (Constants.DEBUG) {
				Log.d(TAG, "listView.invalidateViews() called");
			}
			listAdapter.getAdapter(typeId).notifyDataSetChanged();
			listAdapter.getAdapter(typeId).notifyDataSetInvalidated();
			listAdapter.getListView(typeId).invalidateViews();
		}
	}

	private class ListAdapter {
		private MessageAdapter inboxAdapter;
		private MessageAdapter sentAdapter;
		private MessageAdapter friendsMessageAdapter;
		private MessageAdapter friendsAdapter;
		private ListView inboxView;
		private ListView sentView;
		private ListView friendsMessageView;
		private ListView friendsView;

		public ListAdapter() {
			inboxAdapter = null;
			sentAdapter = null;
			friendsMessageAdapter = null;
			inboxView = null;
			sentView = null;
			friendsMessageView = null;
		}

		public MessageAdapter getAdapter(int typeId) {
			switch (typeId) {
			case INBOX:
				return inboxAdapter;
			case SENT:
				return sentAdapter;
			case FRIEND_MESSAGES:
				return friendsMessageAdapter;
			case FRIENDS:
				return friendsAdapter;
			}
			return null;
		}

		public void setAdapter(MessageAdapter a, int typeId) {
			switch (typeId) {
			case INBOX:
				inboxAdapter = a;
				break;
			case SENT:
				sentAdapter = a;
				break;
			case FRIEND_MESSAGES:
				friendsMessageAdapter = a;
				break;
			case FRIENDS:
				friendsAdapter = a;
				break;
			}
		}

		public ListView getListView(int typeId) {
			switch (typeId) {
			case INBOX:
				return inboxView;
			case SENT:
				return sentView;
			case FRIEND_MESSAGES:
				return friendsMessageView;
			case FRIENDS:
				return friendsView;
			}
			return null;
		}

		public void setListView(ListView l, int typeId) {
			switch (typeId) {
			case INBOX:
				inboxView = l;
				break;
			case SENT:
				sentView = l;
				break;
			case FRIEND_MESSAGES:
				friendsMessageView = l;
				break;
			case FRIENDS:
				friendsView = l;
				break;
			}
		}

	}

	private void logout() {
		if (Constants.DEBUG) {
			Log.d(TAG, "logout() called");
		}
		setContentView(R.layout.login);

		findViewById(R.id.progressBar1).setVisibility(View.INVISIBLE);
		findViewById(R.id.login_button).setOnClickListener(
				new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						if (mBoundService != null) {
							findViewById(R.id.progressBar1).setVisibility(
									View.VISIBLE);
							mBoundService
									.requestLogin(
											((EditText) findViewById(R.id.username_text))
													.getText().toString(),
											((EditText) findViewById(R.id.password_text))
													.getText().toString());
						} else {
							if (Constants.DEBUG) {
								Log.d(TAG, "login clicked - service not bound");
							}
						}
					}
				});

		findViewById(R.id.cancel_button).setOnClickListener(
				new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						finish();
					}
				});

		refreshMenu();

	}

	@SuppressLint("NewApi")
	private void refreshMenu() {
		if (Constants.DEBUG) {
			Log.d(TAG, "refreshMenu() called");
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			invalidateOptionsMenu();
		}
	}

	@Override
	public void onPostFailure(final String message) {
		if (Constants.DEBUG) {
			Log.d(TAG, "onPostFailure called");
		}
		handler.post(new Runnable() {
			public void run() {
				webErrorDialog("Error", "Ok", "Exit", message);
			}
		});
	}

	Handler handler = new Handler();

	@Override
	public void onLoginSuccess() {
		if (Constants.DEBUG) {
			Log.d(TAG, "onLoginSuccess called");
		}
		handler.post(new Runnable() {
			public void run() {
				login();
			}
		});
	}

	@Override
	public void onLoginFailure(final String message) {
		if (Constants.DEBUG) {
			Log.d(TAG, "onLoginFailure called");
		}
		handler.post(new Runnable() {
			public void run() {
				findViewById(R.id.progressBar1).setVisibility(View.INVISIBLE);
				webErrorDialog("Error", "Ok", "Exit", message);
			}
		});
	}

	@Override
	public void onInboxUpdate() {
		if (Constants.DEBUG) {
			Log.d(TAG, "onInboxUpdate called");
		}
		handler.post(new Runnable() {
			public void run() {
				pDialog.dismiss();
				mBoundService.cancelNotify();
				listViewRebuild(INBOX);
			}
		});
	}

	@Override
	public void onFriendsUpdate() {
		handler.post(new Runnable() {
			public void run() {
				listViewRebuild(FRIENDS);
			}
		});
	}

	@Override
	public void onFriendsMessages() {
		handler.post(new Runnable() {
			public void run() {
				listViewRebuild(FRIEND_MESSAGES);
			}
		});
	}

	@Override
	public void onPostSuccess() {
		if (Constants.DEBUG) {
			Log.d(TAG, "onPostSuccess called");
		}
		handler.post(new Runnable() {
			public void run() {
				toastText("successfully posted message");
				mBoundService.requestSent();
			}
		});
	}

	@Override
	public void onMentions() {
		// TODO Auto-generated method stub

	}

	@Override
	public void onSentUpdate() {
		handler.post(new Runnable() {
			public void run() {
				listViewRebuild(SENT);
			}
		});
	}

	@Override
	public void onAddFriendSuccess() {
		// TODO Auto-generated method stub

	}

	@Override
	public void onRemoveFriendSuccess() {
		// TODO Auto-generated method stub

	}

	@Override
	public void onAddFriendFailure(String message) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onRemoveFriendFailure(String message) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onModMailUpdate() {
		// TODO Auto-generated method stub

	}

	@Override
	public void onLogoutSuccess() {
		if (Constants.DEBUG) {
			Log.d(TAG, "onLogoutSuccess called");
		}
		handler.post(new Runnable() {
			public void run() {
				logout();
			}
		});
	}

	private class MessageAdapter extends BaseAdapter {
		private LayoutInflater mInflater;
		protected MessageViewHolder messageHolder;
		private int messageType;

		public MessageAdapter(Context context, int messageType) {
			if (Constants.DEBUG) {
				Log.d(TAG, "InboxAdapter created");
			}
			mInflater = LayoutInflater.from(context);
			this.messageType = messageType;
		}

		@Override
		public int getCount() {
			switch (messageType) {
			case INBOX:
				return mBoundService.getMessages().size();
			case SENT:
				return mBoundService.getSentMessages().size();
			case FRIEND_MESSAGES:
				return mBoundService.getFriendsMessages().size();
			case FRIENDS:
				return mBoundService.getFriends().size();
			}
			return 0;
		}

		@Override
		public Object getItem(int position) {
			return position;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			switch (messageType) {
			case FRIENDS:
				convertView = mInflater
						.inflate(R.layout.friend_list_item, null);

				messageHolder = new MessageViewHolder();
				messageHolder.friend_name = (TextView) convertView
						.findViewById(R.id.friend_name);
				break;
			default:
				convertView = mInflater.inflate(R.layout.message_list_item,
						null);

				messageHolder = new MessageViewHolder();
				messageHolder.sender = (TextView) convertView
						.findViewById(R.id.inbox_message_sender);
				messageHolder.content = (TextView) convertView
						.findViewById(R.id.inbox_message_text);
				messageHolder.time = (TextView) convertView
						.findViewById(R.id.inbox_message_time);
				messageHolder.title = (TextView) convertView
						.findViewById(R.id.inbox_message_title);
				break;
			}

			convertView.setTag(messageHolder);

			switch (messageType) {
			case INBOX:
				messageHolder.rm = mBoundService.getMessages().get(position);
				messageHolder.sender.setText(messageHolder.rm.getSender());
				if (mBoundService.isFriend(messageHolder.rm.getSender())) {
					messageHolder.sender.setTextColor(Color.RED);
				}
				if (mBoundService.isPM(messageHolder.rm)) {
					messageHolder.title.setText("pm: "
							+ messageHolder.rm.getSubject());
				} else {
					messageHolder.title.setText("via: "
							+ messageHolder.rm.getSubreddit());
				}
				if (messageHolder.rm.isNew()) {
					convertView.setBackgroundColor(Color.rgb(247, 190, 129));
				}
				break;
			case SENT:
				messageHolder.rm = mBoundService.getSentMessages()
						.get(position);
				messageHolder.sender.setText(messageHolder.rm.getRecipient());
				if (mBoundService.isFriend(messageHolder.rm.getRecipient())) {
					messageHolder.sender.setTextColor(Color.RED);
				}
				messageHolder.title.setText("pm: "
						+ messageHolder.rm.getSubject());
				break;
			case FRIEND_MESSAGES:
				messageHolder.rm = mBoundService.getFriendsMessages().get(
						position);
				messageHolder.sender.setText(messageHolder.rm.getSender());
				messageHolder.sender.setTextColor(Color.RED);
				messageHolder.title.setText("via: "
						+ messageHolder.rm.getSubreddit());
				break;
			case FRIENDS:
				messageHolder.rf = mBoundService.getFriends().get(position);
				messageHolder.friend_name.setText(messageHolder.rf.getFriend());
				messageHolder.friend_name.setTextColor(Color.RED);
				break;
			}

			switch (messageType) {
			case FRIENDS:
				break;
			default:
				messageHolder.content.setText(Html.fromHtml(
						messageHolder.rm.getContent()).toString());
				messageHolder.time.setText((new Date(((long) Float
						.parseFloat(messageHolder.rm.getTime())) * 1000))
						.toLocaleString());
				break;
			}

			return convertView;
		}

	}

	protected class MessageViewHolder {
		TextView sender;
		TextView recipient;
		TextView title;
		TextView content;
		TextView time;
		TextView friend_name;
		RedditFriends rf;
		RedditMessages rm;
	}

	@Override
	public void onInboxFailure(final String message) {
		if (Constants.DEBUG) {
			Log.d(TAG, "onInboxFailure called");
		}
		handler.post(new Runnable() {
			public void run() {
				webErrorDialog("Error", "Ok", "Exit", message);
			}
		});
	}

}