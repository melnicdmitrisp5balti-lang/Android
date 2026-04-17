package com.parentalcontrol.app.data.database;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.parentalcontrol.app.data.model.SessionEntity;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class SessionDao_Impl implements SessionDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<SessionEntity> __insertionAdapterOfSessionEntity;

  private final SharedSQLiteStatement __preparedStmtOfCloseSession;

  public SessionDao_Impl(RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfSessionEntity = new EntityInsertionAdapter<SessionEntity>(__db) {
      @Override
      public String createQuery() {
        return "INSERT OR REPLACE INTO `Sessions` (`id`,`child_device_id`,`parent_device_id`,`connection_code`,`connected_at`,`disconnected_at`) VALUES (nullif(?, 0),?,?,?,?,?)";
      }

      @Override
      public void bind(SupportSQLiteStatement stmt, SessionEntity value) {
        stmt.bindLong(1, value.getId());
        if (value.getChildDeviceId() == null) {
          stmt.bindNull(2);
        } else {
          stmt.bindString(2, value.getChildDeviceId());
        }
        if (value.getParentDeviceId() == null) {
          stmt.bindNull(3);
        } else {
          stmt.bindString(3, value.getParentDeviceId());
        }
        if (value.getConnectionCode() == null) {
          stmt.bindNull(4);
        } else {
          stmt.bindString(4, value.getConnectionCode());
        }
        stmt.bindLong(5, value.getConnectedAt());
        if (value.getDisconnectedAt() == null) {
          stmt.bindNull(6);
        } else {
          stmt.bindLong(6, value.getDisconnectedAt());
        }
      }
    };
    this.__preparedStmtOfCloseSession = new SharedSQLiteStatement(__db) {
      @Override
      public String createQuery() {
        final String _query = "UPDATE sessions SET disconnected_at = ? WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final SessionEntity session, final Continuation<? super Long> continuation) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          long _result = __insertionAdapterOfSessionEntity.insertAndReturnId(session);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, continuation);
  }

  @Override
  public Object closeSession(final long sessionId, final long disconnectedAt,
      final Continuation<? super Unit> continuation) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfCloseSession.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, disconnectedAt);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, sessionId);
        __db.beginTransaction();
        try {
          _stmt.executeUpdateDelete();
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
          __preparedStmtOfCloseSession.release(_stmt);
        }
      }
    }, continuation);
  }

  @Override
  public Object getActiveByCode(final String connectionCode,
      final Continuation<? super SessionEntity> continuation) {
    final String _sql = "SELECT * FROM sessions WHERE connection_code = ? AND disconnected_at IS NULL";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (connectionCode == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, connectionCode);
    }
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<SessionEntity>() {
      @Override
      public SessionEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfChildDeviceId = CursorUtil.getColumnIndexOrThrow(_cursor, "child_device_id");
          final int _cursorIndexOfParentDeviceId = CursorUtil.getColumnIndexOrThrow(_cursor, "parent_device_id");
          final int _cursorIndexOfConnectionCode = CursorUtil.getColumnIndexOrThrow(_cursor, "connection_code");
          final int _cursorIndexOfConnectedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "connected_at");
          final int _cursorIndexOfDisconnectedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "disconnected_at");
          final SessionEntity _result;
          if(_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpChildDeviceId;
            if (_cursor.isNull(_cursorIndexOfChildDeviceId)) {
              _tmpChildDeviceId = null;
            } else {
              _tmpChildDeviceId = _cursor.getString(_cursorIndexOfChildDeviceId);
            }
            final String _tmpParentDeviceId;
            if (_cursor.isNull(_cursorIndexOfParentDeviceId)) {
              _tmpParentDeviceId = null;
            } else {
              _tmpParentDeviceId = _cursor.getString(_cursorIndexOfParentDeviceId);
            }
            final String _tmpConnectionCode;
            if (_cursor.isNull(_cursorIndexOfConnectionCode)) {
              _tmpConnectionCode = null;
            } else {
              _tmpConnectionCode = _cursor.getString(_cursorIndexOfConnectionCode);
            }
            final long _tmpConnectedAt;
            _tmpConnectedAt = _cursor.getLong(_cursorIndexOfConnectedAt);
            final Long _tmpDisconnectedAt;
            if (_cursor.isNull(_cursorIndexOfDisconnectedAt)) {
              _tmpDisconnectedAt = null;
            } else {
              _tmpDisconnectedAt = _cursor.getLong(_cursorIndexOfDisconnectedAt);
            }
            _result = new SessionEntity(_tmpId,_tmpChildDeviceId,_tmpParentDeviceId,_tmpConnectionCode,_tmpConnectedAt,_tmpDisconnectedAt);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, continuation);
  }

  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
