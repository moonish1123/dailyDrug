package com.networkmodule.internal.adapter

import com.networkmodule.api.NetworkResult
import com.networkmodule.internal.util.ErrorMapper
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import okhttp3.Request
import okio.Timeout
import retrofit2.Call
import retrofit2.CallAdapter
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit

internal class NetworkResultCallAdapterFactory(
    private val errorMapper: ErrorMapper
) : CallAdapter.Factory() {

    override fun get(
        returnType: Type,
        annotations: Array<out Annotation>,
        retrofit: Retrofit
    ): CallAdapter<*, *>? {
        val rawType = getRawType(returnType)
        if (rawType != NetworkResult::class.java) return null
        require(returnType is ParameterizedType) {
            "NetworkResult must be parameterized as NetworkResult<Foo>"
        }
        val successType = getParameterUpperBound(0, returnType)
        return NetworkResultCallAdapter<Any>(successType, errorMapper)
    }

    private class NetworkResultCallAdapter<R>(
        private val successType: Type,
        private val errorMapper: ErrorMapper
    ) : CallAdapter<R, Call<NetworkResult<R>>> {

        override fun responseType(): Type = successType

        override fun adapt(call: Call<R>): Call<NetworkResult<R>> {
            return NetworkResultCall(call, errorMapper, successType)
        }
    }
}

private class NetworkResultCall<R>(
    private val delegate: Call<R>,
    private val errorMapper: ErrorMapper,
    private val successType: Type
) : Call<NetworkResult<R>> {

    override fun enqueue(callback: Callback<NetworkResult<R>>) {
        delegate.enqueue(object : Callback<R> {
            override fun onResponse(call: Call<R>, response: Response<R>) {
                val result = errorMapper.toNetworkResult(response, successType)
                callback.onResponse(this@NetworkResultCall, Response.success(result))
            }

            override fun onFailure(call: Call<R>, t: Throwable) {
                val errorResult = NetworkResult.Error(errorMapper.mapFailure(t))
                callback.onResponse(this@NetworkResultCall, Response.success(errorResult))
            }
        })
    }

    override fun isExecuted(): Boolean = delegate.isExecuted

    override fun clone(): Call<NetworkResult<R>> =
        NetworkResultCall(delegate.clone(), errorMapper, successType)

    override fun isCanceled(): Boolean = delegate.isCanceled

    override fun cancel() = delegate.cancel()

    override fun execute(): Response<NetworkResult<R>> {
        return try {
            val response = delegate.execute()
            val result = errorMapper.toNetworkResult(response, successType)
            Response.success(result)
        } catch (t: Throwable) {
            val errorResult = NetworkResult.Error(errorMapper.mapFailure(t))
            Response.success(errorResult)
        }
    }

    override fun request(): Request = delegate.request()

    override fun timeout(): Timeout = delegate.timeout()
}
