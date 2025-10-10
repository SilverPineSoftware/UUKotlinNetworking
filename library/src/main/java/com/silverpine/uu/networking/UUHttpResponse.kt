package com.silverpine.uu.networking

import com.silverpine.uu.core.UUError
import java.net.HttpURLConnection

open class UUHttpResponse(
    val request: UUHttpRequest,
    val response: HttpURLConnection? = null,
    val headers: UUHttpHeaders? = response?.headerFields?.let { UUHttpHeaders(it) },
    val error: UUError? = null,
    val parsedResponse: Any? = null,
    //val rawResponse: ByteArray? = null,
    val endTime: Long = System.currentTimeMillis()
)
{
    //var success: SuccessType? = null
    //var error: UUError? = null

//    var httpCode: Int = 0
//    var contentType: String = ""
//    var contentEncoding: String = ""
//    val headers: UUHttpHeaders = UUHttpHeaders()

    val httpStatusCode: Int
        get()
        {
            return response?.responseCode ?: 0
        }
}

/*
public class UUHttpResponse : NSObject
{
    public let httpRequest: UUHttpRequest
    public let httpResponse: HTTPURLResponse?
    public let httpError: Error?
    public let parsedResponse: Any?
    public let rawResponse: Data?
    public let endTime: TimeInterval

    required init(
    request: UUHttpRequest,
    response: HTTPURLResponse? = nil,
    error: Error? = nil,
    rawResponse: Data? = nil,
    parsedResponse: Any? = nil)
    {
        self.httpRequest = request
        self.httpResponse = response
        self.httpError = error
        self.rawResponse = rawResponse
        self.parsedResponse = parsedResponse
        self.endTime = Date.timeIntervalSinceReferenceDate
    }

    // MARK: Computed Variables

    public var httpStatusCode: Int
    {
        return httpResponse?.statusCode ?? 0
    }
}
*/
/*
open class UUHttpResponse<SuccessType, ErrorType>(
    val httpRequest: UUHttpRequest<SuccessType, ErrorType>,
    val httpResponse: HttpURLConnection? = null,
    val httpError: Exception? = null,
    val rawResponse: ByteArray? = null,
    val parsedResponse: Any? = null
) {
    val endTime: Long = System.currentTimeMillis()

    // Computed property equivalent to Swift’s `httpStatusCode`
    val httpStatusCode: Int
        get() = httpResponse?.responseCode ?: 0
}*/

open class UUTypedHttpResponse<SuccessType: Any, ErrorType: Any>(
    val request: UUTypedHttpRequest<SuccessType, ErrorType>)
{
    var success: SuccessType? = null
    var error: UUError? = null

    var httpCode: Int = 0
    var contentType: String = ""
    var contentEncoding: String = ""
    val headers: UUHttpHeaders = UUHttpHeaders()
}






/*
public protocol UUHttpDataParser
{
    func parse(data: Data, response: HTTPURLResponse, request: URLRequest, completion: @escaping (Any?)->())
}

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


/*
public protocol UUHttpResponseHandler
{
    func handleResponse(request: UUHttpRequest, data: Data?, response: URLResponse?, error: Error?, completion: @escaping (UUHttpResponse)->())

    var successParser: UUHttpDataParser { get }
    var errorParser: UUHttpDataParser { get }
}

open class UUBaseResponseHandler: UUHttpResponseHandler
{
    public required init()
    {

    }

    open var successParser: UUHttpDataParser
    {
        return UUMimeTypeDataParser()
    }

    open var errorParser: UUHttpDataParser
    {
        return UUMimeTypeDataParser()
    }

    open func handleResponse(request: UUHttpRequest, data: Data?, response: URLResponse?, error: Error?, completion: @escaping (UUHttpResponse)->())
    {
        if let e = error
                {
                    UULog.debug(tag: LOG_TAG, message: "Got an error: \(String(describing: error))")
                    let err = UUErrorFactory.wrapNetworkError(e, request)
                    finishHandleResponse(request: request, response: response, data: data, result: err, completion: completion)
                    return
                }

        guard let httpResponse = response as? HTTPURLResponse else
        {
            let err = UUErrorFactory.createError(UUHttpSessionError.unkownError, [:])
            finishHandleResponse(request: request, response: response, data: data, result: err, completion: completion)
            return
        }

        UULog.debug(tag: LOG_TAG, message: "HTTP Response Code: \(httpResponse.statusCode)")

        httpResponse.allHeaderFields.forEach()
        { (key: AnyHashable, value: Any) in
            UULog.debug(tag: LOG_TAG, message: "ResponseHeader: \(key) - \(value)")
        }

        // Verify there is response data to parse, if not, just finish the operation
        guard let data = data,
        !data.isEmpty,
        let httpResponse = response as? HTTPURLResponse,
        let urlRequest = request.httpRequest else
        {
            finishHandleResponse(request: request, response: response, data: data, result: nil, completion: completion)
            return
        }

        UULog.debug(tag: LOG_TAG, message: "ResponseBody: \(String(describing: String(bytes: data, encoding: .utf8)))")

        let parser = httpResponse.statusCode.uuIsHttpSuccess() ? successParser : errorParser

        parser.parse(data: data, response: httpResponse, request: urlRequest)
        { parseResult in

                self.finishHandleResponse(request: request, response: httpResponse, data: data, result: parseResult, completion: completion)
        }
    }

    private func finishHandleResponse(request: UUHttpRequest, response: URLResponse?, data: Data?, result: Any?, completion: @escaping (UUHttpResponse)->())
    {
        var err: Error? = nil
        var parsedResponse: Any? = result

        if let parseError = result as? Error
                {
                    err = parseError
                    parsedResponse = nil
                }

        let httpResponse = (response as? HTTPURLResponse)
        let httpStatusCode = httpResponse?.statusCode ?? 0

        // By default, the standard response parsers won't emit an Error, but custom response handlers might.
        // When callers parse response JSON and return Errors, we will honor that.
        if (err == nil && !isHttpSuccessResponseCode(httpStatusCode))
        {
            err = UUErrorFactory.createHttpError(request, httpStatusCode, parsedResponse)
        }

        let uuResponse = UUHttpResponse(request: request, response: httpResponse, error: err, rawResponse: data, parsedResponse: parsedResponse)
        completion(uuResponse)
    }

    private func isHttpSuccessResponseCode(_ responseCode : Int) -> Bool
    {
        return (responseCode >= 200 && responseCode < 300)
    }
}

open class UUJsonCodableResponseHandler<SuccessType: Codable, ErrorType: Codable>: UUBaseResponseHandler
{
    public required init()
    {
        super.init()
    }

    public var jsonDecoder: JSONDecoder = JSONDecoder()

    open override var successParser: UUHttpDataParser
    {
        let parser = UUJsonCodableDataParser<SuccessType>()
        parser.jsonDecoder = self.jsonDecoder
        return parser
    }

    open override var errorParser: UUHttpDataParser
    {
        let parser = UUJsonCodableDataParser<ErrorType>()
        parser.jsonDecoder = self.jsonDecoder
        return parser
    }
}

open class UUPassthroughResponseHandler: UUBaseResponseHandler
{
    public required init()
    {
        super.init()
    }

    open override var successParser: UUHttpDataParser
    {
        return UUBinaryDataParser()
    }

    open override var errorParser: UUHttpDataParser
    {
        return UUBinaryDataParser()
    }
}
*/
