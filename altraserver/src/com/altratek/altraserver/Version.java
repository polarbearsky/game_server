package com.altratek.altraserver;

public class Version {
	public static final String version = "2016.06.02_02";

	// TODO: features:
	// 3. buffer���ԣ�direct, default init size�����á�
	// 4. destroy����չ��˳��
	// 5. lost�Ӹ���չ�߳���???
	// 6. user lost���򴥷���
	// 7. ����extension��Ϣ�������	

	// + + + + + + + + + + + + + + change log + + + + + + + + + + + + + + +
	
	// - - - - - - - - - - - - - - - - - 2016.06.01 - - - - - - - - - - - - - - - - -
	// 1. ������ص�����(process config:�˿ڣ�zone��logLevel��)֧���Զ����ȡ
	// 2. ����zone�ļ���
	// 3. �����ⲿ�����÷�����ȡ
	
	// - - - - - - - - - - - - - - - - - 2015.05.15 - - - - - - - - - - - - - - - - -
	// 1. �������User���󴴽���IUserBuilder�ӿڣ��Լ�Ĭ�ϵ�ʵ����DefaultUserBuilder
	// 2. ֧���Զ���User�����Զ���������̳����е�User���󣬲��ұ�д��ӦUserBuilder��ʵ��IUserBuilder
	// 3. �ļ�Config.xml������<UserBuilder>���ֵ����ΪIUserBuilderʵ�����·����û�д����ý����ἤ��User�Զ��幦��

	// - - - - - - - - - - - - - - - - - 2014.08.25 - - - - - - - - - - - - - - - - -
	// 1. ������Ϣ���ܿ��Ÿ���չ���ơ�
	// 2. ���к�Ϊ0����Ϣ�����ܡ���Ϊ�˼�˹���Ա�ͻ��ˣ�
	// 3. �޸�����ͬid���ܵ�¼ͬһ���̡����⡣

	// - - - - - - - - - - - - - - - - - 2014.01.03 - - - - - - - - - - - - - - - - -
	// 1. ���������ð����кż򵥼��ܣ����İ���ҡ�
	// 2. ��д���и�ΪArrayBlockingQueue���ڴ������Ż���gcƵ���½�20%��throughout���������½�(������ԭ��)��
	// 3. ����ActionscriptObject.getInt������Χ���⡣
	// 4. ������Ϣ������и�ΪArrayBlockingQueue��
	// 5. ɾ���û����ַ����˺ͳ��ȼ�顣
	// 6. ���벻���ڷ�����־�޸ģ����ڿͻ����ų�����
	// 7. ��������������־�ļ��𣬼���warn��־���š�

	// - - - - - - - - - - - - - - - - - 2013.12.05 - - - - - - - - - - - - - - - - -
	// 1. ���ӷ���������״̬��أ��ƶ�֮ǰ��ConfigData��û�õ�ͳ�Ƽ�����
	// 2. ActionscriptObject����byte array copy�Ż���
	// 3. �ط�����user lost�¼���
	// 4. login����Ǩ�ƣ�AltraServer --> ChannelManager��
	// 5. ����һ����ʷ����MessageValidator��Server���ã�����Zone���á�
	// 6. LostHandler�̳߳ش�AltraServer���롣

	// - - - - - - - - - - - - - - - - - 2013.10.10 - - - - - - - - - - - - - - - - -
	// 1. ������Ϣ�������xxxHandler����ִ���̳߳�InMsgHandlerWorkPool���롣
	// ������Ϊ�л��̳߳���׼����
	// 2. �����Ƿ���Ҫϵͳ�̣߳����Ƿ������Ϣ���ͷ���Ϣ�����̳߳ء�
	// 3. ��MessageChannel������Ϣ�����ߣ���Ϣ���������Ͳ��پ�����SocketChannel��User��
	// 4. �޸�δ��������jar֮��ط�class loader���⡣
	// 5. ip������֧���Զ������ã����ݿ⣩��reload��
	// 6. �Ҳ�����չ����־��request command��

	// - - - - - - - - - - - - - - - - - 2013.08.23 - - - - - - - - - - - - - - - - -
	// 1. ����ͨ��alsadmin.jar���͹���Ա��Ϣ��ȡ��stop���͹���Ա��Ϣ��
	// 2. ȡ��Parse XtReqMessage������Ϣ���ݡ�
	// 3. �޸Ĺط���IS_SHUTTING_DOWN֮ǰsleep��Ϊ֮��sleep��֮ǰsleepû���ˡ�
	// 4. ���ö�ȡ�����쳣�����Ż���
	// 5. ��չbyte cmd����Ҳ�����չ����Ϊ����ң��Ͽ����ӡ�
	// 6. ΪsessionKey����msg seq noУ����׼�����޸ģ���

	// - - - - - - - - - - - - - - - - - 2013.08.16 - - - - - - - - - - - - - - - - -
	// 1. ɾ��User���õĳ�Ա������
	// 2. ��Session��User����lost��־������
	// 3. AltraServer��ChannelSelector����Ǩ�ơ�
	// 4. lost conn��check unlogin channel������select�߳�����
	// 5. ��AltraServer��zone��channel��صĴ���ֱ��ȡ������
	// 6. ��User��дʧ����صı����Ƶ�session�С�
	// 7. �޸�schedule�Լ�ɾ�����õ�schedule task��
	// 8. ���в���ReponseMsg���ͣ��򻯹���Ա��Ϣ��
	// 9. �ع��û�������ExtensionHelper��SystemHandler���ظ����롣

	// - - - - - - - - - - - - - - - - - 2013.08.09 - - - - - - - - - - - - - - - - -
	// 1. ����ServerEvent��׼��������չ��Ϣ���С�
	// 2. ȡ��֮ǰServerEvent����RequestEvent���ȴ�ʱ���profile��Ϣ��
	// 3. ����SystemEvent�Ĵ����SystemHandler���������������SystemEventDispatcher��
	// 4. ȡ��logout��
	// 5. ��ServerEvent����User���ԡ�
	// 6. ȡ��һ��AltraServerȫ��list clients����˶����˺ܶ����̡�
	// 7. �ع�ConnClearTimerTask���룬ȡ������Ա�����ж���û��Ҫ��ɴ��븴���ԡ�
	// 8. �Ż�һ��accept new connection Ƶ������LinkedList��������⡣
	// 9. �ع�zone�����ֵ���롣
	// 10. ��¼lostBottomHalfExecutorService����ȴ����еĳ��ȡ�

	// - - - - - - - - - - - - - - - - - 2013.08.02 - - - - - - - - - - - - - - - - -
	// 1. �ڼ���������Ϣ�������ֲ��Ϸ��ĵط������ӶϿ����ӵĲ�����
	// 2. �ع���ExtensionHandler�Ĵ󷽷�processEvent��
	// 3. �ع��˹���Ա������Ϣ����ĵط�����AltraServer��������
	// 4. ����Ϊ�Ƿ���Ϣ��¼�����־��

	// - - - - - - - - - - - - - - - - - 2013.04.16 - - - - - - - - - - - - - - - - -
	// 1. ������������cross domain���ӶϿ���ip���������ٵ�bug��
	// 2. ��������ע�������ӵ�bug��
	// 3. �ṩ������Ϣ���к�������չ���ƣ�ȡ����Ϣͷsalt byte��
	// 4. sendResponse LinkedList<SocketChannel>�޸�ΪList<SocketChannel>��
	// 5. ������ExtensionHelper��AbstractExtension��sendResponse���ظ����롣
	// 6. logDisMatchMsgNum�����û�����id��
	// 7. ȡ���ڲ�sendResponse��sender������
	// 8. zone�洢���û���Ϣ��User�滻id��
	// 9. �Ż�ϵͳ��Ϣ���ͽ����߲������ݡ�

	// - - - - - - - - - - - - - - - - - 2013.04.03 - - - - - - - - - - - - - - - - -
	// 1. ���ӷ���sendResponse(ActionscriptObject, User, List<User>)�����ڵ�������˵���ṩList<SocketChannel>�����ѡ�
	// 2. ��ӡ����ActionscriptObject�����ԡ�

	// - - - - - - - - - - - - - - - - - 2013.03.11 - - - - - - - - - - - - - - - - -
	// 1. ����IN_MSG_BUFFER_LEN��2^15 -> 2^13�����а�����С����ͳ�Ƶġ�
	// 2. �ع���չ���ش��롣
	// 3. ��������������չ��Ϣ��̫���ˣ�ˢ��̫�죬�ڸ���һ������error��Ϣ��

	// - - - - - - - - - - - - - - - - - 2013.03.01 - - - - - - - - - - - - - - - - -
	// 1. ����selectNow + no sleepģʽ����Ƶ��select���µ�GCƵ��(2sһ��)���⡣selectNow��������
	// 2. ������������ӡ�
	// 3. ��accept connect�����AltraServer��ȡ������
	// 4. ��select�����AltraServer��ȡ������
	// 5. ����accept new connectionʱip��������return���º���connection��ʧbug��

	// - - - - - - - - - - - - - - - - - 2013.02.21 - - - - - - - - - - - - - - - - -
	// 1. lost connection����reason��������־���������ϲ��Է����Բ������ߡ�
	// 2. select�̲߳�sleep�ˣ�IO select�����������ģ��������cpu���ܡ�

	// - - - - - - - - - - - - - - - - - 2013.02.02 - - - - - - - - - - - - - - - - -
	// 1. ��Attachment�����ΪSession���������ݵĽ�ɫԭ��Խ��Ҫ�ˣ�������һ���������ˡ���
	// 2. ��д����ȥ��Ϣ�������ߣ���¼д����ȥ�Ĵ�����
	// 3. �Ѽ�����ɬ����ͷ�����������Ϊÿ�ο����붼Ҫ����һ�²��ܿ�����
	// ----a. EventHandler�ӿ�ɾ�����������û�ô���
	// ----b. EventWrap -> InMsghandler, EventWrapWorker -> InMsghandlerWorker
	// ----c. MessageHandlerɾ�����������û�á�
	// ----d. ChannelWrapɾ����ֱ������ServerWriter��ChannelWrapWorker -> OutMsgHandlerWorker
	// ----e.������Ϣ�����з���handleEvent -> acceptEvent����handleEvent��processEvent����һ����ֲ��壩
	// ----f. ������Ϣ�����з���handleChannel -> sendOutMsg��дsocket����processEvent -> writeOutMsg
	// 4.�û��̶߳��зֲ�����΢����SocketChannel.hashCode % ThreadNum -> AutoIncrementInt % ThreadNum
	// 5. ɾ�����ڲ����ԡ��������ֵĴ����Moderator��ش��롣
	// 6. �Ż�SystemHandler����û�������Ĵ��� : ��ɢ��� -> ���м�顣
	// 7. ɾ�����������
	// 8. �û�����д����bug����������Ƚϸ��ӣ�����ȡ����������һ�����ֻ��оͼ��롱����Դ������һ���汾���ģ���

	// - - - - - - - - - - - - - - - - - 2013.01.26 - - - - - - - - - - - - - - - - -
	// 1. ���û����·���Ϣ�б�����Attachment��
	// 2. һ��socket writeδд��İ����Ϣ��������Ϣ�б���ó��������ø������档
	// 3. 1,2�޸�֮��OutgoingServerEvent��<SocketChannel, List<OutgoingServerEvent>>û�б�Ҫ�����ˣ�ɾ����
	// 4. ServerWriter��������е�����ȡ����������һ�����ֻ��оͼ��롱����Դ��
	// 5. Version change log�����ڵ�����
	// 6. �û�������Ϣ������ArrayDeque�滻LinkedList��
	// 7. Ⱥ�������ȡ�����û�socket channel�б���ArrayList��
	// 8. ����RequestMessage parse�����ĵ��õ����ࡣ

	// - - - - - - - - - - - - - - - - - 2013.01.18 - - - - - - - - - - - - - - - - -
	// 1. �޸�������Ϣ���ݽ�������������buffer�Ƿ�direct�ж��ͼ�bug��
	// 2. �޸�room���û��б�洢����һ��id list��name list����һ��user list��

	// - - - - - - - - - - - - - - - - - 2013.01.11 - - - - - - - - - - - - - - - - -
	// 1. ������Ϣ��������Ϣ������롣
	// 2. ������Ϣ���ݽ�������������buffer�Ƿ�direct��
	// 3. crossdomain������Ϣȡ�������־������ֱ������������ʵ�֡�
	// 4. ȡ��������ϢServerEvent��װ��ֱ��ί�ɸ�ResponseMessage������ServerEvent�������ڴ�����
	// 5. ServerEvent.action��int -> short

	// - - - - - - - - - - - - - - - - - 2013.01.04 - - - - - - - - - - - - - - - - -
	// 1. ��ȡ�����İ汾���ļ�Version.java��
	// 2. �޸�Room.getAllUsersArray����null user��bug��
	// 3. ����"Dropped outgoing message - User[%s]"log levelΪdebug��error̫���ˣ�û�����塣
	// 4. ����������Ϣ��Ĭ��buffer size��1024 -> 124
	// 5. ������Ϣȡ��direct buffer������heap buffer
}