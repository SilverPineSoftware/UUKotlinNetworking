package com.silverpine.uu.networking

import com.silverpine.uu.core.UUJson
import com.silverpine.uu.core.uuReadAll
import java.io.InputStream
import java.net.HttpURLConnection

fun interface UUHttpStreamParser
{
    fun parse(
        stream: InputStream,
        response: HttpURLConnection
    ): Any?
}

open class UUBinaryStreamParser: UUHttpStreamParser
{
    override fun parse(
        stream: InputStream,
        response: HttpURLConnection
    ): Any?
    {
        return stream.uuReadAll()
    }
}

class UUTextResponseParser: UUHttpStreamParser
{
    override fun parse(stream: InputStream, response: HttpURLConnection): Any?
    {
        //response.contentType
        //response.contentEncoding

        // Encoding and Charset
        val bytes = stream.uuReadAll() ?: return null
        return String(bytes)
    }
}

open class UUTypedStreamParser<DataType: Any>(private val objectClass: Class<DataType>): UUHttpStreamParser
{
    override fun parse(
        stream: InputStream,
        response: HttpURLConnection
    ): Any?
    {
        return UUJson.fromStream(stream, objectClass).getOrNull()
    }
}



/*
open class UUTextDataParser: UUHttpDataParser
{
    public required init()
    {

    }

    open func parse(data: Data, response: HTTPURLResponse, request: URLRequest, completion: @escaping (Any?)->())
    {
        var parsed : Any? = nil

        var responseEncoding : String.Encoding = .utf8

        if (response.textEncodingName != nil)
        {
            let cfEncoding = CFStringConvertIANACharSetNameToEncoding(response.textEncodingName as CFString?)
            responseEncoding = String.Encoding(rawValue: CFStringConvertEncodingToNSStringEncoding(cfEncoding))
        }

        let stringResult : String? = String.init(data: data, encoding: responseEncoding)
        if (stringResult != nil)
        {
            parsed = stringResult
        }

        completion(parsed)
    }
}

open class UUBinaryDataParser: UUHttpDataParser
{
    public required init()
    {

    }

    open func parse(data: Data, response: HTTPURLResponse, request: URLRequest, completion: @escaping (Any?)->())
    {
        completion(data)
    }
}

open class UUJsonDataParser: UUHttpDataParser
{
    public required init()
    {

    }

    open func parse(data: Data, response: HTTPURLResponse, request: URLRequest, completion: @escaping (Any?)->())
    {
        var result: Any? = nil

        do
        {
            result = try JSONSerialization.jsonObject(with: data, options: [.fragmentsAllowed])
            }
            catch (let err)
            {
                UULog.debug(tag: LOG_TAG, message: "Error deserializing JSON: \(String(describing: err))")
            }

            completion(result)
        }
}

open class UUImageDataParser: UUHttpDataParser
{
    public required init()
    {

    }

    open func parse(data: Data, response: HTTPURLResponse, request: URLRequest, completion: @escaping (Any?)->())
    {
        completion(UUImage(data: data))
    }
}

open class UUFormEncodedDataParser: UUHttpDataParser
{
    public required init()
    {

    }

    open func parse(data: Data, response: HTTPURLResponse, request: URLRequest, completion: @escaping (Any?)->())
    {
        var parsed: [ String: Any ] = [:]

        if let s = String.init(data: data, encoding: .utf8)
        {
            let components = s.components(separatedBy: "&")

            for c in components
            {
                let pair = c.components(separatedBy: "=")

                if pair.count == 2
                {
                    if let key = pair.first
                            {
                                if let val = pair.last
                                {
                                    parsed[key] = val.removingPercentEncoding
                                }
                            }
                }
            }
        }

        completion(parsed)
    }
}

open class UUMimeTypeDataParser: UUHttpDataParser
{
    private var parsers: [String:UUHttpDataParser] = [:]

    public required init()
    {
        registerResponseHandler([UUContentType.applicationJson, UUContentType.textJson], UUJsonDataParser())
        registerResponseHandler([UUContentType.textHtml, UUContentType.textPlain], UUTextDataParser())
        registerResponseHandler([UUContentType.binary], UUBinaryDataParser())
        registerResponseHandler([UUContentType.imagePng, UUContentType.imageJpeg], UUImageDataParser())
        registerResponseHandler([UUContentType.formEncoded], UUFormEncodedDataParser())
    }

    public func registerResponseHandler(_ mimeTypes: [String], _ parser: UUHttpDataParser)
    {
        for mimeType in mimeTypes
        {
            parsers[mimeType] = parser
        }
    }

    open func parse(data: Data, response: HTTPURLResponse, request: URLRequest, completion: @escaping (Any?)->())
    {
        guard let mimeType = response.mimeType else
        {
            completion(nil)
            return
        }

        guard let parser = parsers[mimeType] else
        {
            completion(nil)
            return
        }

        parser.parse(data: data, response: response, request: request, completion: completion)
    }
}


open class UUJsonCodableDataParser<T: Codable>: UUHttpDataParser
{
    public required init()
    {

    }

    public var jsonDecoder: JSONDecoder = JSONDecoder()

    open func parse(data: Data, response: HTTPURLResponse, request: URLRequest, completion: @escaping (Any?)->())
    {
        var result: Any? = nil

        do
        {
            result = try jsonDecoder.decode(T.self, from: data)
            }
            catch let err
            {
                result = UUErrorFactory.createParseError(err, data, response, request)
            }

            completion(result)
        }
}
*/