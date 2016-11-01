package com.altratek.altraserver;

public class Version {
	public static final String version = "2016.06.02_02";

	// TODO: features:
	// 3. buffer属性（direct, default init size）配置。
	// 4. destroy按扩展倒顺序。
	// 5. lost扔给扩展线程做???
	// 6. user lost倒序触发。
	// 7. 公开extension消息队列入口	

	// + + + + + + + + + + + + + + change log + + + + + + + + + + + + + + +
	
	// - - - - - - - - - - - - - - - - - 2016.06.01 - - - - - - - - - - - - - - - - -
	// 1. 进程相关的配置(process config:端口，zone，logLevel等)支持自定义读取
	// 2. 简化了zone的加载
	// 3. 加载外部类重用方法抽取
	
	// - - - - - - - - - - - - - - - - - 2015.05.15 - - - - - - - - - - - - - - - - -
	// 1. 添加用于User对象创建的IUserBuilder接口，以及默认的实现类DefaultUserBuilder
	// 2. 支持自定义User对象，自定义对象必须继承现有的User对象，并且编写对应UserBuilder类实现IUserBuilder
	// 3. 文件Config.xml中增加<UserBuilder>项，其值配置为IUserBuilder实现类的路径，没有此配置将不会激活User自定义功能

	// - - - - - - - - - - - - - - - - - 2014.08.25 - - - - - - - - - - - - - - - - -
	// 1. 上行消息加密开放给扩展定制。
	// 2. 序列号为0的消息不加密。（为了兼顾管理员客户端）
	// 3. 修复“相同id不能登录同一进程”问题。

	// - - - - - - - - - - - - - - - - - 2014.01.03 - - - - - - - - - - - - - - - - -
	// 1. 上行数据用包序列号简单加密，防改包外挂。
	// 2. 把写队列改为ArrayBlockingQueue。内存略有优化，gc频率下降20%，throughout理论上有下降(锁策略原因)。
	// 3. 修正ActionscriptObject.getInt整数范围问题。
	// 4. 上行消息处理队列改为ArrayBlockingQueue。
	// 5. 删除用户名字符过滤和长度检查。
	// 6. 加入不存在房间日志修改，便于客户端排除错误。
	// 7. 调整几个常见日志的级别，减少warn日志干扰。

	// - - - - - - - - - - - - - - - - - 2013.12.05 - - - - - - - - - - - - - - - - -
	// 1. 增加服务器运行状态监控，移动之前在ConfigData里没用的统计计数。
	// 2. ActionscriptObject序列byte array copy优化。
	// 3. 关服触发user lost事件。
	// 4. login方法迁移，AltraServer --> ChannelManager。
	// 5. 纠正一个历史错误：MessageValidator变Server配置，而非Zone配置。
	// 6. LostHandler线程池从AltraServer剥离。

	// - - - - - - - - - - - - - - - - - 2013.10.10 - - - - - - - - - - - - - - - - -
	// 1. 上行消息处理程序xxxHandler和其执行线程池InMsgHandlerWorkPool分离。
	// 分离是为切换线程池做准备。
	// 2. 配置是否需要系统线程，即是否根据消息类型分消息处理线程池。
	// 3. 用MessageChannel抽象消息接受者，消息接受者类型不再局限于SocketChannel和User。
	// 4. 修复未重启更换jar之后关服class loader问题。
	// 5. ip白名单支持自定义配置（数据库）和reload。
	// 6. 找不到扩展加日志加request command。

	// - - - - - - - - - - - - - - - - - 2013.08.23 - - - - - - - - - - - - - - - - -
	// 1. 增加通过alsadmin.jar发送管理员消息，取消stop发送管理员消息。
	// 2. 取消Parse XtReqMessage错误消息内容。
	// 3. 修改关服。IS_SHUTTING_DOWN之前sleep改为之后sleep，之前sleep没用了。
	// 4. 配置读取部分异常处理优化。
	// 5. 扩展byte cmd如果找不到扩展，认为是外挂，断开链接。
	// 6. 为sessionKey参与msg seq no校验做准备（修改）。

	// - - - - - - - - - - - - - - - - - 2013.08.16 - - - - - - - - - - - - - - - - -
	// 1. 删除User无用的成员变量。
	// 2. 在Session和User增加lost标志变量。
	// 3. AltraServer和ChannelSelector方法迁移。
	// 4. lost conn和check unlogin channel都纳入select线程做。
	// 5. 把AltraServer中zone和channel相关的代码分别抽取出来。
	// 6. 把User中写失败相关的变量移到session中。
	// 7. 修改schedule以及删除无用的schedule task。
	// 8. 集中部分ReponseMsg发送，简化管理员消息。
	// 9. 重构用户变量在ExtensionHelper和SystemHandler的重复代码。

	// - - - - - - - - - - - - - - - - - 2013.08.09 - - - - - - - - - - - - - - - - -
	// 1. 泛化ServerEvent，准备开放扩展消息队列。
	// 2. 取消之前ServerEvent（现RequestEvent）等待时间的profile信息。
	// 3. 触发SystemEvent的代码从SystemHandler抽离出来单独的类SystemEventDispatcher。
	// 4. 取消logout。
	// 5. 在ServerEvent加入User属性。
	// 6. 取消一个AltraServer全局list clients，因此而简化了很多流程。
	// 7. 重构ConnClearTimerTask代码，取消管理员链接判定，没必要造成代码复杂性。
	// 8. 优化一个accept new connection 频繁产生LinkedList对象的问题。
	// 9. 重构zone配置项赋值代码。
	// 10. 记录lostBottomHalfExecutorService任务等待队列的长度。

	// - - - - - - - - - - - - - - - - - 2013.08.02 - - - - - - - - - - - - - - - - -
	// 1. 在几处上行消息解析发现不合法的地方，增加断开链接的操作。
	// 2. 重构了ExtensionHandler的大方法processEvent。
	// 3. 重构了管理员配置信息保存的地方，从AltraServer类抽出来。
	// 4. 单独为非法消息记录外挂日志。

	// - - - - - - - - - - - - - - - - - 2013.04.16 - - - - - - - - - - - - - - - - -
	// 1. 修正开发环境cross domain链接断开，ip计数不减少的bug。
	// 2. 修正批量注册新链接的bug。
	// 3. 提供上行消息序列号生成扩展定制，取消消息头salt byte。
	// 4. sendResponse LinkedList<SocketChannel>修改为List<SocketChannel>。
	// 5. 消除了ExtensionHelper和AbstractExtension里sendResponse的重复代码。
	// 6. logDisMatchMsgNum增加用户名和id。
	// 7. 取消内部sendResponse的sender参数。
	// 8. zone存储的用户信息用User替换id。
	// 9. 优化系统消息发送接受者参数传递。

	// - - - - - - - - - - - - - - - - - 2013.04.03 - - - - - - - - - - - - - - - - -
	// 1. 增加方法sendResponse(ActionscriptObject, User, List<User>)，对于调用者来说，提供List<SocketChannel>较困难。
	// 2. 打印下行ActionscriptObject供调试。

	// - - - - - - - - - - - - - - - - - 2013.03.11 - - - - - - - - - - - - - - - - -
	// 1. 调整IN_MSG_BUFFER_LEN，2^15 -> 2^13，上行包都很小。有统计的。
	// 2. 重构扩展加载代码。
	// 3. 不再输出房间和扩展信息，太多了，刷屏太快，掩盖了一下其他error信息。

	// - - - - - - - - - - - - - - - - - 2013.03.01 - - - - - - - - - - - - - - - - -
	// 1. 修正selectNow + no sleep模式，过频繁select导致的GC频繁(2s一次)问题。selectNow不阻塞。
	// 2. 批量添加新链接。
	// 3. 把accept connect代码从AltraServer提取出来。
	// 4. 把select代码从AltraServer提取出来。
	// 5. 修正accept new connection时ip超过限制return导致后续connection丢失bug。

	// - - - - - - - - - - - - - - - - - 2013.02.21 - - - - - - - - - - - - - - - - -
	// 1. lost connection加上reason，记入日志，用于线上测试服调试不明掉线。
	// 2. select线程不sleep了，IO select操作是阻塞的，不会造成cpu空跑。

	// - - - - - - - - - - - - - - - - - 2013.02.02 - - - - - - - - - - - - - - - - -
	// 1. 把Attachment类改名为Session，这个类扮演的角色原来越重要了，不再是一个“附件了”。
	// 2. 因写不出去消息而被断线，记录写不出去的次数。
	// 3. 把几个晦涩的类和方法改名，因为每次看代码都要整理一下才能看懂。
	// ----a. EventHandler接口删除，这个抽象没用处。
	// ----b. EventWrap -> InMsghandler, EventWrapWorker -> InMsghandlerWorker
	// ----c. MessageHandler删除，这个基类没用。
	// ----d. ChannelWrap删除，直接移至ServerWriter；ChannelWrapWorker -> OutMsgHandlerWorker
	// ----e.上行消息进队列方法handleEvent -> acceptEvent。（handleEvent和processEvent摆在一起，真分不清）
	// ----f. 下行消息进队列方法handleChannel -> sendOutMsg；写socket方法processEvent -> writeOutMsg
	// 4.用户线程队列分布策略微调：SocketChannel.hashCode % ThreadNum -> AutoIncrementInt % ThreadNum
	// 5. 删除了内部禁言、过滤脏字的处理和Moderator相关代码。
	// 6. 优化SystemHandler检查用户和区域的代码 : 分散检查 -> 集中检查。
	// 7. 删除无用配置项。
	// 8. 用户进入写队列bug调整，这个比较复杂，不能取消“处理完一个发现还有就加入”的来源（在上一个版本做的）。

	// - - - - - - - - - - - - - - - - - 2013.01.26 - - - - - - - - - - - - - - - - -
	// 1. 把用户的下发消息列表移至Attachment。
	// 2. 一次socket write未写完的半截消息不放在消息列表里，拿出来单独用个变量存。
	// 3. 1,2修改之后，OutgoingServerEvent和<SocketChannel, List<OutgoingServerEvent>>没有必要存在了，删除。
	// 4. ServerWriter的任务队列的生产取消“处理完一个发现还有就加入”的来源。
	// 5. Version change log按日期倒排序。
	// 6. 用户下行消息队列用ArrayDeque替换LinkedList。
	// 7. 群发房间获取房间用户socket channel列表用ArrayList。
	// 8. 调整RequestMessage parse方法的调用到基类。

	// - - - - - - - - - - - - - - - - - 2013.01.18 - - - - - - - - - - - - - - - - -
	// 1. 修改下行消息根据接受者数量决定buffer是否direct判定低级bug。
	// 2. 修改room的用户列表存储，把一个id list和name list换成一个user list。

	// - - - - - - - - - - - - - - - - - 2013.01.11 - - - - - - - - - - - - - - - - -
	// 1. 上行消息和下行消息基类分离。
	// 2. 下行消息根据接受者数量决定buffer是否direct。
	// 3. crossdomain下行消息取消基类标志变量，直接由子类特殊实现。
	// 4. 取消下行消息ServerEvent包装，直接委派给ResponseMessage，减少ServerEvent带来的内存消耗
	// 5. ServerEvent.action：int -> short

	// - - - - - - - - - - - - - - - - - 2013.01.04 - - - - - - - - - - - - - - - - -
	// 1. 提取单独的版本号文件Version.java。
	// 2. 修改Room.getAllUsersArray返回null user的bug。
	// 3. 调整"Dropped outgoing message - User[%s]"log level为debug，error太多了，没有意义。
	// 4. 调正下行消息的默认buffer size：1024 -> 124
	// 5. 下行消息取消direct buffer，换成heap buffer
}