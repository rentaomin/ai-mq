// 当前类为对外提供访问的API controllerm,CredidCardAccountApplicationApi 为OpenAPI Specification自动生成的API
 @RestController
 public class CreditCardApplicationController implements CredidCardAccountApplicationApi {
	private final PostFlow postFlow;
	
	public CreditCardApplicationController(PostFlow postFlow){
		this.postFlow = postFlow
	}
	
	// ResponseEntity 为spring 对象，PostRequestSchema 和 PostResponseSchema 和为项目根据OpenAPI yaml 自动生成的请求参数对象和接口响应对象
	@Override 
	public ResponseEntity<PostResponseSchema> createCreditCardAccountApplication(RequestContext<PostRequestSchema> requestContext) throw Exception {
	  return ResponseEntity.status(201).body(postFlow.process(requestContext))
	}
 }
 
 
 
 // 当前为 kotlin 业务入口处理文件，flowSercice 为公司封装组件的类
 @Component
 open class PostFlow(flowSercice:FlowSercice){
 
	// 当前的方法为固定模板，其中仅值为变量
   var flow = flowSercice.createMqSendAndReceiveFlow(
	   mqConfiguration = MqConfiguration(
	      clientName ="default",
	      requestQueue = "wmq.jms.queue.out",
		  responseQueue = "wmq.jms.queue.in"
	   ),
	   ismConfiguration = IsmConfiguration(
	     transformerName = "post",
		 ism20Envelopebuilder = postEnvelopeBuilder
	   ),
	   requestMapper = postRequestMapper,
	   responseMapper = postResponseMapper,
	   backendResponseValidator = postBackendResponseValidator
   
   )
   // 当前入参和反参说明等同于前面controller说明
   fun process(requestContext:RequestContext<PostRequestSchema>) = flow.process(requestContext)
 }
 
 
 
 // kotlin文件，主要封装mq 请求 request header 报文对象，其余均为内置字段，下述出现的为变量值
 typealias PostEnvelopeBuilder = BiFunction<Config,RequestContext<PostRequestSchema>,Envelope>
 val postEnvelopeBuilder: PostEnvelopeBuilder = PostEnvelopeBuilder {config,rc ->
	ism20Envelope {
	    ismHeader {
		 // ism20Header.ismHdrConsumerId 环境变量取值
		  consumerId = config.get("ism20Header.ismHdrConsumerId")
		}
		opHeader {
		 
		 // 对应shared header 中的内容
		   appExtGrp {
		      seq = 1
		      appExtn {
			     item= 1
				 type = "OH_SERVICE_HEADER"
				 userData = "0100000   xx"
			  }
			  appExtn {
			     item =2 
				 type = "OH_PROCESS_SERVICE_HEADER"
				 userData = "0100000  xx"
			  }
			  appExtn {
			     item = 3
				 type = "SWHCB_APPLICATION_HEADER"
				 userData = "010000  xx"
			  }
		   }
		}
	}
 }
 
 // 固定模板用于将 OpenAPI Specification 生成的PostRequestSchema 映射到jave bean对象，构建MQ请求 payload报文,
 // CreateApplicationRequest 根据excel的内容生成的java bean request 对象
  typealias PostRequestMapper = BiFunction<Config,RequestContext<PostRequestSchema>,CreateApplicationRequest>
 
  val postRequestMapper: PostRequestMapper = PostRequestMapper{_,rc ->
		val p = rc.payload()
		CreateApplicationRequest().apply{
		   createApp = CreateApplication().apply{
		     domicileBranch = p .createApp?.domicileBranch
		   }
		}
		
		//
		...
  }
  
  
  
  // 固定模板处理将MQ响应报文java bean 对象 CreateApplicationResponse 映射到OpenAPI Specification 生成的API 响应对象PostResponseSchema 中，最终返回给用户
  typealias PostResponseMapper = TriFunction<Config,RequestContext<PostRequestSchema>,CreateApplicationResponse,PostResponseSchema>
 
  val postResponseMapper: PostResponseMapper = PostResponseMapper{_,_,p ->
		val p = rc.payload()
		PostResponseSchema().apply{
		  responseCde = PostResponseSchemaResponsesCde().apply{
		    responseCode = p.responseCde.responseCode
		  }
		}
		
		//
		...
  
  }
 
 
 // 用于校验响应结果，处理业务异常
   val postBackendResponseValidator: BackendResponseValidator<CreateApplicationResponse> = BackendResponseValidator{config,requestContext,payload ->
		val responseCode = payload.responseCde.responseCode.toString()
		val validationResult = SilverResponseValidator.validate("post",config,responseCode)
		if(validationResult.isFailed()){
		  throw Exception("xx")
		
		}
		
		//
		...
  
  }