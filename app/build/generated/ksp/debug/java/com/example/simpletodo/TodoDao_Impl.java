package com.example.simpletodo;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class TodoDao_Impl implements TodoDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<TodoItem> __insertionAdapterOfTodoItem;

  private final Converters __converters = new Converters();

  private final EntityDeletionOrUpdateAdapter<TodoItem> __deletionAdapterOfTodoItem;

  private final EntityDeletionOrUpdateAdapter<TodoItem> __updateAdapterOfTodoItem;

  public TodoDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfTodoItem = new EntityInsertionAdapter<TodoItem>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `todo_items` (`id`,`name`,`timeInMillis`,`isMonthly`,`remindCount`,`isDone`,`remarks`,`imagePaths`,`maxRetries`,`retryIntervalHours`,`repeatMode`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final TodoItem entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindLong(3, entity.getTimeInMillis());
        final int _tmp = entity.isMonthly() ? 1 : 0;
        statement.bindLong(4, _tmp);
        statement.bindLong(5, entity.getRemindCount());
        final int _tmp_1 = entity.isDone() ? 1 : 0;
        statement.bindLong(6, _tmp_1);
        statement.bindString(7, entity.getRemarks());
        final String _tmp_2 = __converters.fromList(entity.getImagePaths());
        statement.bindString(8, _tmp_2);
        statement.bindLong(9, entity.getMaxRetries());
        statement.bindLong(10, entity.getRetryIntervalHours());
        statement.bindLong(11, entity.getRepeatMode());
      }
    };
    this.__deletionAdapterOfTodoItem = new EntityDeletionOrUpdateAdapter<TodoItem>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `todo_items` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final TodoItem entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__updateAdapterOfTodoItem = new EntityDeletionOrUpdateAdapter<TodoItem>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `todo_items` SET `id` = ?,`name` = ?,`timeInMillis` = ?,`isMonthly` = ?,`remindCount` = ?,`isDone` = ?,`remarks` = ?,`imagePaths` = ?,`maxRetries` = ?,`retryIntervalHours` = ?,`repeatMode` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final TodoItem entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindLong(3, entity.getTimeInMillis());
        final int _tmp = entity.isMonthly() ? 1 : 0;
        statement.bindLong(4, _tmp);
        statement.bindLong(5, entity.getRemindCount());
        final int _tmp_1 = entity.isDone() ? 1 : 0;
        statement.bindLong(6, _tmp_1);
        statement.bindString(7, entity.getRemarks());
        final String _tmp_2 = __converters.fromList(entity.getImagePaths());
        statement.bindString(8, _tmp_2);
        statement.bindLong(9, entity.getMaxRetries());
        statement.bindLong(10, entity.getRetryIntervalHours());
        statement.bindLong(11, entity.getRepeatMode());
        statement.bindLong(12, entity.getId());
      }
    };
  }

  @Override
  public Object insert(final TodoItem todo, final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfTodoItem.insertAndReturnId(todo);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object delete(final TodoItem todo, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfTodoItem.handle(todo);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object update(final TodoItem todo, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfTodoItem.handle(todo);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<TodoItem>> getAllTodos() {
    final String _sql = "SELECT * FROM todo_items ORDER BY timeInMillis ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"todo_items"}, new Callable<List<TodoItem>>() {
      @Override
      @NonNull
      public List<TodoItem> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfTimeInMillis = CursorUtil.getColumnIndexOrThrow(_cursor, "timeInMillis");
          final int _cursorIndexOfIsMonthly = CursorUtil.getColumnIndexOrThrow(_cursor, "isMonthly");
          final int _cursorIndexOfRemindCount = CursorUtil.getColumnIndexOrThrow(_cursor, "remindCount");
          final int _cursorIndexOfIsDone = CursorUtil.getColumnIndexOrThrow(_cursor, "isDone");
          final int _cursorIndexOfRemarks = CursorUtil.getColumnIndexOrThrow(_cursor, "remarks");
          final int _cursorIndexOfImagePaths = CursorUtil.getColumnIndexOrThrow(_cursor, "imagePaths");
          final int _cursorIndexOfMaxRetries = CursorUtil.getColumnIndexOrThrow(_cursor, "maxRetries");
          final int _cursorIndexOfRetryIntervalHours = CursorUtil.getColumnIndexOrThrow(_cursor, "retryIntervalHours");
          final int _cursorIndexOfRepeatMode = CursorUtil.getColumnIndexOrThrow(_cursor, "repeatMode");
          final List<TodoItem> _result = new ArrayList<TodoItem>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final TodoItem _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final long _tmpTimeInMillis;
            _tmpTimeInMillis = _cursor.getLong(_cursorIndexOfTimeInMillis);
            final boolean _tmpIsMonthly;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsMonthly);
            _tmpIsMonthly = _tmp != 0;
            final int _tmpRemindCount;
            _tmpRemindCount = _cursor.getInt(_cursorIndexOfRemindCount);
            final boolean _tmpIsDone;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsDone);
            _tmpIsDone = _tmp_1 != 0;
            final String _tmpRemarks;
            _tmpRemarks = _cursor.getString(_cursorIndexOfRemarks);
            final List<String> _tmpImagePaths;
            final String _tmp_2;
            _tmp_2 = _cursor.getString(_cursorIndexOfImagePaths);
            _tmpImagePaths = __converters.fromString(_tmp_2);
            final int _tmpMaxRetries;
            _tmpMaxRetries = _cursor.getInt(_cursorIndexOfMaxRetries);
            final int _tmpRetryIntervalHours;
            _tmpRetryIntervalHours = _cursor.getInt(_cursorIndexOfRetryIntervalHours);
            final int _tmpRepeatMode;
            _tmpRepeatMode = _cursor.getInt(_cursorIndexOfRepeatMode);
            _item = new TodoItem(_tmpId,_tmpName,_tmpTimeInMillis,_tmpIsMonthly,_tmpRemindCount,_tmpIsDone,_tmpRemarks,_tmpImagePaths,_tmpMaxRetries,_tmpRetryIntervalHours,_tmpRepeatMode);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getAllTodosList(final Continuation<? super List<TodoItem>> $completion) {
    final String _sql = "SELECT * FROM todo_items ORDER BY timeInMillis ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<TodoItem>>() {
      @Override
      @NonNull
      public List<TodoItem> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfTimeInMillis = CursorUtil.getColumnIndexOrThrow(_cursor, "timeInMillis");
          final int _cursorIndexOfIsMonthly = CursorUtil.getColumnIndexOrThrow(_cursor, "isMonthly");
          final int _cursorIndexOfRemindCount = CursorUtil.getColumnIndexOrThrow(_cursor, "remindCount");
          final int _cursorIndexOfIsDone = CursorUtil.getColumnIndexOrThrow(_cursor, "isDone");
          final int _cursorIndexOfRemarks = CursorUtil.getColumnIndexOrThrow(_cursor, "remarks");
          final int _cursorIndexOfImagePaths = CursorUtil.getColumnIndexOrThrow(_cursor, "imagePaths");
          final int _cursorIndexOfMaxRetries = CursorUtil.getColumnIndexOrThrow(_cursor, "maxRetries");
          final int _cursorIndexOfRetryIntervalHours = CursorUtil.getColumnIndexOrThrow(_cursor, "retryIntervalHours");
          final int _cursorIndexOfRepeatMode = CursorUtil.getColumnIndexOrThrow(_cursor, "repeatMode");
          final List<TodoItem> _result = new ArrayList<TodoItem>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final TodoItem _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final long _tmpTimeInMillis;
            _tmpTimeInMillis = _cursor.getLong(_cursorIndexOfTimeInMillis);
            final boolean _tmpIsMonthly;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsMonthly);
            _tmpIsMonthly = _tmp != 0;
            final int _tmpRemindCount;
            _tmpRemindCount = _cursor.getInt(_cursorIndexOfRemindCount);
            final boolean _tmpIsDone;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsDone);
            _tmpIsDone = _tmp_1 != 0;
            final String _tmpRemarks;
            _tmpRemarks = _cursor.getString(_cursorIndexOfRemarks);
            final List<String> _tmpImagePaths;
            final String _tmp_2;
            _tmp_2 = _cursor.getString(_cursorIndexOfImagePaths);
            _tmpImagePaths = __converters.fromString(_tmp_2);
            final int _tmpMaxRetries;
            _tmpMaxRetries = _cursor.getInt(_cursorIndexOfMaxRetries);
            final int _tmpRetryIntervalHours;
            _tmpRetryIntervalHours = _cursor.getInt(_cursorIndexOfRetryIntervalHours);
            final int _tmpRepeatMode;
            _tmpRepeatMode = _cursor.getInt(_cursorIndexOfRepeatMode);
            _item = new TodoItem(_tmpId,_tmpName,_tmpTimeInMillis,_tmpIsMonthly,_tmpRemindCount,_tmpIsDone,_tmpRemarks,_tmpImagePaths,_tmpMaxRetries,_tmpRetryIntervalHours,_tmpRepeatMode);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getTodoById(final long id, final Continuation<? super TodoItem> $completion) {
    final String _sql = "SELECT * FROM todo_items WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<TodoItem>() {
      @Override
      @Nullable
      public TodoItem call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfTimeInMillis = CursorUtil.getColumnIndexOrThrow(_cursor, "timeInMillis");
          final int _cursorIndexOfIsMonthly = CursorUtil.getColumnIndexOrThrow(_cursor, "isMonthly");
          final int _cursorIndexOfRemindCount = CursorUtil.getColumnIndexOrThrow(_cursor, "remindCount");
          final int _cursorIndexOfIsDone = CursorUtil.getColumnIndexOrThrow(_cursor, "isDone");
          final int _cursorIndexOfRemarks = CursorUtil.getColumnIndexOrThrow(_cursor, "remarks");
          final int _cursorIndexOfImagePaths = CursorUtil.getColumnIndexOrThrow(_cursor, "imagePaths");
          final int _cursorIndexOfMaxRetries = CursorUtil.getColumnIndexOrThrow(_cursor, "maxRetries");
          final int _cursorIndexOfRetryIntervalHours = CursorUtil.getColumnIndexOrThrow(_cursor, "retryIntervalHours");
          final int _cursorIndexOfRepeatMode = CursorUtil.getColumnIndexOrThrow(_cursor, "repeatMode");
          final TodoItem _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final long _tmpTimeInMillis;
            _tmpTimeInMillis = _cursor.getLong(_cursorIndexOfTimeInMillis);
            final boolean _tmpIsMonthly;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsMonthly);
            _tmpIsMonthly = _tmp != 0;
            final int _tmpRemindCount;
            _tmpRemindCount = _cursor.getInt(_cursorIndexOfRemindCount);
            final boolean _tmpIsDone;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsDone);
            _tmpIsDone = _tmp_1 != 0;
            final String _tmpRemarks;
            _tmpRemarks = _cursor.getString(_cursorIndexOfRemarks);
            final List<String> _tmpImagePaths;
            final String _tmp_2;
            _tmp_2 = _cursor.getString(_cursorIndexOfImagePaths);
            _tmpImagePaths = __converters.fromString(_tmp_2);
            final int _tmpMaxRetries;
            _tmpMaxRetries = _cursor.getInt(_cursorIndexOfMaxRetries);
            final int _tmpRetryIntervalHours;
            _tmpRetryIntervalHours = _cursor.getInt(_cursorIndexOfRetryIntervalHours);
            final int _tmpRepeatMode;
            _tmpRepeatMode = _cursor.getInt(_cursorIndexOfRepeatMode);
            _result = new TodoItem(_tmpId,_tmpName,_tmpTimeInMillis,_tmpIsMonthly,_tmpRemindCount,_tmpIsDone,_tmpRemarks,_tmpImagePaths,_tmpMaxRetries,_tmpRetryIntervalHours,_tmpRepeatMode);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
