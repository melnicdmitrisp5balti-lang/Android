package com.parentalcontrol.app.data.database;

import android.database.Cursor;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.parentalcontrol.app.data.model.ConnectionCode;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.Collections;
import java.util.List;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class ConnectionCodeDao_Impl implements ConnectionCodeDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<ConnectionCode> __insertionAdapterOfConnectionCode;

  private final SharedSQLiteStatement __preparedStmtOfDeactivateAll;

  public ConnectionCodeDao_Impl(RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfConnectionCode = new EntityInsertionAdapter<ConnectionCode>(__db) {
      @Override
      public String createQuery() {
        return "INSERT OR REPLACE INTO `ConnectionCodes` (`id`,`code`,`device_id`,`parent_id`,`created_at`,`is_active`) VALUES (nullif(?, 0),?,?,?,?,?)";
      }

      @Override
      public void bind(SupportSQLiteStatement stmt, ConnectionCode value) {
        stmt.bindLong(1, value.getId());
        if (value.getCode() == null) {
          stmt.bindNull(2);
        } else {
          stmt.bindString(2, value.getCode());
        }
        if (value.getDeviceId() == null) {
          stmt.bindNull(3);
        } else {
          stmt.bindString(3, value.getDeviceId());
        }
        if (value.getParentId() == null) {
          stmt.bindNull(4);
        } else {
          stmt.bindString(4, value.getParentId());
        }
        stmt.bindLong(5, value.getCreatedAt());
        final int _tmp = value.isActive() ? 1 : 0;
        stmt.bindLong(6, _tmp);
      }
    };
    this.__preparedStmtOfDeactivateAll = new SharedSQLiteStatement(__db) {
      @Override
      public String createQuery() {
        final String _query = "UPDATE connection_codes SET is_active = 0";
        return _query;
      }
    };
  }

  @Override
  public long insert(final ConnectionCode code) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      long _result = __insertionAdapterOfConnectionCode.insertAndReturnId(code);
      __db.setTransactionSuccessful();
      return _result;
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void deactivateAll() {
    __db.assertNotSuspendingTransaction();
    final SupportSQLiteStatement _stmt = __preparedStmtOfDeactivateAll.acquire();
    __db.beginTransaction();
    try {
      _stmt.executeUpdateDelete();
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
      __preparedStmtOfDeactivateAll.release(_stmt);
    }
  }

  @Override
  public ConnectionCode getActiveCode() {
    final String _sql = "SELECT * FROM connection_codes WHERE is_active = 1 LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public ConnectionCode getActiveByCode(final String code) {
    final String _sql = "SELECT * FROM connection_codes WHERE code = ? AND is_active = 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (code == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, code);
    }
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
