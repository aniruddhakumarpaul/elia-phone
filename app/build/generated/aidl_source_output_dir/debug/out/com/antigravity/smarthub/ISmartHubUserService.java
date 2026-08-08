/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package com.antigravity.smarthub;
public interface ISmartHubUserService extends android.os.IInterface
{
  /** Default implementation for ISmartHubUserService. */
  public static class Default implements com.antigravity.smarthub.ISmartHubUserService
  {
    @Override public int setRefreshRateMode(int mode) throws android.os.RemoteException
    {
      return 0;
    }
    @Override public int setStandbyBucket(java.lang.String packageName, java.lang.String bucket) throws android.os.RemoteException
    {
      return 0;
    }
    @Override public int setAppOpsBackground(java.lang.String packageName, java.lang.String mode) throws android.os.RemoteException
    {
      return 0;
    }
    @Override public java.lang.String readSetting(java.lang.String table, java.lang.String key) throws android.os.RemoteException
    {
      return null;
    }
    @Override public int readStandbyBucket(java.lang.String packageName) throws android.os.RemoteException
    {
      return 0;
    }
    @Override public java.lang.String readAppOpsBackground(java.lang.String packageName) throws android.os.RemoteException
    {
      return null;
    }
    @Override public void destroy() throws android.os.RemoteException
    {
    }
    @Override
    public android.os.IBinder asBinder() {
      return null;
    }
  }
  /** Local-side IPC implementation stub class. */
  public static abstract class Stub extends android.os.Binder implements com.antigravity.smarthub.ISmartHubUserService
  {
    /** Construct the stub at attach it to the interface. */
    public Stub()
    {
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an com.antigravity.smarthub.ISmartHubUserService interface,
     * generating a proxy if needed.
     */
    public static com.antigravity.smarthub.ISmartHubUserService asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof com.antigravity.smarthub.ISmartHubUserService))) {
        return ((com.antigravity.smarthub.ISmartHubUserService)iin);
      }
      return new com.antigravity.smarthub.ISmartHubUserService.Stub.Proxy(obj);
    }
    @Override public android.os.IBinder asBinder()
    {
      return this;
    }
    @Override public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException
    {
      java.lang.String descriptor = DESCRIPTOR;
      if (code >= android.os.IBinder.FIRST_CALL_TRANSACTION && code <= android.os.IBinder.LAST_CALL_TRANSACTION) {
        data.enforceInterface(descriptor);
      }
      switch (code)
      {
        case INTERFACE_TRANSACTION:
        {
          reply.writeString(descriptor);
          return true;
        }
      }
      switch (code)
      {
        case TRANSACTION_setRefreshRateMode:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _result = this.setRefreshRateMode(_arg0);
          reply.writeNoException();
          reply.writeInt(_result);
          break;
        }
        case TRANSACTION_setStandbyBucket:
        {
          java.lang.String _arg0;
          _arg0 = data.readString();
          java.lang.String _arg1;
          _arg1 = data.readString();
          int _result = this.setStandbyBucket(_arg0, _arg1);
          reply.writeNoException();
          reply.writeInt(_result);
          break;
        }
        case TRANSACTION_setAppOpsBackground:
        {
          java.lang.String _arg0;
          _arg0 = data.readString();
          java.lang.String _arg1;
          _arg1 = data.readString();
          int _result = this.setAppOpsBackground(_arg0, _arg1);
          reply.writeNoException();
          reply.writeInt(_result);
          break;
        }
        case TRANSACTION_readSetting:
        {
          java.lang.String _arg0;
          _arg0 = data.readString();
          java.lang.String _arg1;
          _arg1 = data.readString();
          java.lang.String _result = this.readSetting(_arg0, _arg1);
          reply.writeNoException();
          reply.writeString(_result);
          break;
        }
        case TRANSACTION_readStandbyBucket:
        {
          java.lang.String _arg0;
          _arg0 = data.readString();
          int _result = this.readStandbyBucket(_arg0);
          reply.writeNoException();
          reply.writeInt(_result);
          break;
        }
        case TRANSACTION_readAppOpsBackground:
        {
          java.lang.String _arg0;
          _arg0 = data.readString();
          java.lang.String _result = this.readAppOpsBackground(_arg0);
          reply.writeNoException();
          reply.writeString(_result);
          break;
        }
        case TRANSACTION_destroy:
        {
          this.destroy();
          reply.writeNoException();
          break;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
      return true;
    }
    private static class Proxy implements com.antigravity.smarthub.ISmartHubUserService
    {
      private android.os.IBinder mRemote;
      Proxy(android.os.IBinder remote)
      {
        mRemote = remote;
      }
      @Override public android.os.IBinder asBinder()
      {
        return mRemote;
      }
      public java.lang.String getInterfaceDescriptor()
      {
        return DESCRIPTOR;
      }
      @Override public int setRefreshRateMode(int mode) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        int _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(mode);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setRefreshRateMode, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readInt();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public int setStandbyBucket(java.lang.String packageName, java.lang.String bucket) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        int _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeString(packageName);
          _data.writeString(bucket);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setStandbyBucket, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readInt();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public int setAppOpsBackground(java.lang.String packageName, java.lang.String mode) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        int _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeString(packageName);
          _data.writeString(mode);
          boolean _status = mRemote.transact(Stub.TRANSACTION_setAppOpsBackground, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readInt();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public java.lang.String readSetting(java.lang.String table, java.lang.String key) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        java.lang.String _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeString(table);
          _data.writeString(key);
          boolean _status = mRemote.transact(Stub.TRANSACTION_readSetting, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readString();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public int readStandbyBucket(java.lang.String packageName) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        int _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeString(packageName);
          boolean _status = mRemote.transact(Stub.TRANSACTION_readStandbyBucket, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readInt();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public java.lang.String readAppOpsBackground(java.lang.String packageName) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        java.lang.String _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeString(packageName);
          boolean _status = mRemote.transact(Stub.TRANSACTION_readAppOpsBackground, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readString();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public void destroy() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_destroy, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
    }
    static final int TRANSACTION_setRefreshRateMode = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_setStandbyBucket = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_setAppOpsBackground = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
    static final int TRANSACTION_readSetting = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
    static final int TRANSACTION_readStandbyBucket = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
    static final int TRANSACTION_readAppOpsBackground = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
    static final int TRANSACTION_destroy = (android.os.IBinder.FIRST_CALL_TRANSACTION + 6);
  }
  public static final java.lang.String DESCRIPTOR = "com.antigravity.smarthub.ISmartHubUserService";
  public int setRefreshRateMode(int mode) throws android.os.RemoteException;
  public int setStandbyBucket(java.lang.String packageName, java.lang.String bucket) throws android.os.RemoteException;
  public int setAppOpsBackground(java.lang.String packageName, java.lang.String mode) throws android.os.RemoteException;
  public java.lang.String readSetting(java.lang.String table, java.lang.String key) throws android.os.RemoteException;
  public int readStandbyBucket(java.lang.String packageName) throws android.os.RemoteException;
  public java.lang.String readAppOpsBackground(java.lang.String packageName) throws android.os.RemoteException;
  public void destroy() throws android.os.RemoteException;
}
