-- =============================================
-- V2__seed_questions.sql - 预置50道八股文题目
-- =============================================

-- ==================== 计算机网络 (8题) ====================
INSERT INTO question (category_id, type, title, options, answer, analysis, difficulty, tags) VALUES

(1, 1, 'OSI七层模型中，传输层的作用是什么？', '[{"label":"A","content":"路由选择"},{"label":"B","content":"端到端可靠传输"},{"label":"C","content":"数据加密"},{"label":"D","content":"物理介质访问"}]', 'B', '传输层负责端到端的可靠数据传输，主要协议有TCP和UDP。TCP提供面向连接、可靠的数据传输；UDP提供无连接、不可靠但高效的数据传输。', 1, 'OSI,TCP,基础'),

(1, 1, 'TCP三次握手中，第二次握手服务器发送的是什么？', '[{"label":"A","content":"SYN"},{"label":"B","content":"ACK"},{"label":"C","content":"SYN+ACK"},{"label":"D","content":"FIN+ACK"}]', 'C', '第二次握手：服务器收到SYN后，回复SYN+ACK报文，表示同意建立连接并确认客户端的SYN。', 1, 'TCP,握手,基础'),

(1, 1, 'HTTP状态码301和302的区别是什么？', '[{"label":"A","content":"301临时重定向，302永久重定向"},{"label":"B","content":"301永久重定向，302临时重定向"},{"label":"C","content":"都是永久重定向"},{"label":"D","content":"都是临时重定向"}]', 'B', '301表示永久重定向，浏览器会缓存并后续自动跳转；302表示临时重定向，每次都会重新请求原URL。对SEO影响不同。', 2, 'HTTP,状态码'),

(1, 1, 'DNS解析中，以下哪个是域名解析的正确顺序？', '[{"label":"A","content":"浏览器缓存→本地hosts→DNS服务器→根域名服务器"},{"label":"B","content":"DNS服务器→根域名服务器→本地hosts→浏览器缓存"},{"label":"C","content":"根域名服务器→DNS服务器→浏览器缓存→本地hosts"},{"label":"D","content":"本地hosts→DNS服务器→根域名服务器→浏览器缓存"}]', 'A', 'DNS解析顺序：浏览器DNS缓存 → 操作系统hosts文件 → 本地DNS服务器 → 根域名服务器 → 顶级域名服务器 → 权威DNS服务器。', 2, 'DNS,网络'),

(1, 3, 'HTTPS使用Symmetric加密来加密传输数据。', NULL, '错误', 'HTTPS使用非对称加密（如RSA/ECDHE）进行密钥交换和身份验证，使用对称加密（如AES）来加密传输数据。两者结合使用。', 1, 'HTTPS,加密'),

(1, 2, '以下哪些是TCP的特点？', '[{"label":"A","content":"面向连接"},{"label":"B","content":"可靠传输"},{"label":"C","content":"流量控制"},{"label":"D","content":"不需要确认"}]', 'A,B,C', 'TCP是面向连接的、可靠的传输协议，提供流量控制、拥塞控制和差错控制。UDP才是不需要确认的。', 1, 'TCP,特性'),

(1, 1, '127.0.0.1是什么地址？', '[{"label":"A","content":"广播地址"},{"label":"B","content":"本地回环地址"},{"label":"C","content":"网关地址"},{"label":"D","content":"子网掩码"}]', 'B', '127.0.0.1是IPv4的本地回环地址（localhost），用于本机网络软件测试和进程间通信。', 1, 'IP,基础'),

(1, 4, 'HTTP协议的默认端口是____，HTTPS的默认端口是____。', NULL, '80,443', 'HTTP使用80端口进行明文通信，HTTPS使用443端口进行加密通信，通过TLS/SSL协议保证安全。', 1, '端口,基础'),

-- ==================== 操作系统 (8题) ====================
(2, 1, '进程和线程的根本区别是什么？', '[{"label":"A","content":"进程更快"},{"label":"B","content":"进程是资源分配的基本单位，线程是调度的基本单位"},{"label":"C","content":"线程需要更多内存"},{"label":"D","content":"没有区别"}]', 'B', '进程是操作系统资源分配的基本单位，每个进程拥有独立的地址空间。线程是CPU调度的基本单位，同一进程的多个线程共享地址空间和资源。', 1, '进程,线程,基础'),

(2, 1, '以下哪种状态不属于进程的基本状态？', '[{"label":"A","content":"就绪状态"},{"label":"B","content":"运行状态"},{"label":"C","content":"阻塞状态"},{"label":"D","content":"完成状态"}]', 'D', '进程的三种基本状态：就绪（Ready）、运行（Running）、阻塞（Blocked/等待）。完成不是基本状态，而是进程终止。', 1, '进程状态,基础'),

(2, 3, '并发就是多个进程在同一时刻同时运行。', NULL, '错误', '并发是指多个进程在宏观上同时运行（交替执行），并行才是真正在同一时刻同时运行（需要多核CPU）。', 1, '并发,并行,基础'),

(2, 1, '什么是内存泄漏？', '[{"label":"A","content":"内存被病毒破坏"},{"label":"B","content":"不再使用的内存没有被释放"},{"label":"C","content":"物理内存物理损坏"},{"label":"D","content":"内存溢出错误"}]', 'B', '内存泄漏（Memory Leak）是指程序申请内存后不再使用，但未释放或无法释放已分配的内存空间，导致可用内存逐渐减少。', 2, '内存管理'),

(2, 1, '虚拟内存的主要作用是什么？', '[{"label":"A","content":"加快硬盘读写速度"},{"label":"B","content":"让程序可以使用比物理内存更大的地址空间"},{"label":"C","content":"增加物理内存容量"},{"label":"D","content":"提高CPU运算速度"}]', 'B', '虚拟内存将磁盘空间模拟为内存使用，使系统可以运行比实际物理内存更大的程序，实现内存扩展和保护。', 1, '虚拟内存,基础'),

(2, 2, '以下哪些是进程间通信方式(IPC)？', '[{"label":"A","content":"管道"},{"label":"B","content":"消息队列"},{"label":"C","content":"共享内存"},{"label":"D","content":"Socket"}]', 'A,B,C,D', '进程间通信方式包括：管道(Pipe)、命名管道(FIFO)、消息队列、共享内存、信号量、信号(Signal)、Socket等。', 2, 'IPC,进程通信'),

(2, 1, '死锁的必要条件不包括以下哪项？', '[{"label":"A","content":"互斥条件"},{"label":"B","content":"请求与保持"},{"label":"C","content":"优先级反转"},{"label":"D","content":"循环等待"}]', 'C', '死锁的四个必要条件：互斥、请求与保持、不可剥夺、循环等待。优先级反转是实时系统中的现象，不是死锁的必要条件。', 3, '死锁,深入'),

(2, 4, 'Linux中使用____命令可以查看进程信息，使用____命令可以杀死进程。', NULL, 'ps,kill', 'ps（process status）显示当前进程快照；kill发送信号给进程终止其运行。常用：ps aux 查看所有进程，kill -9 PID 强制终止。', 1, 'Linux,基础'),

-- ==================== 数据结构 (8题) ====================
(3, 1, '栈的特性是什么？', '[{"label":"A","content":"先进先出"},{"label":"B","content":"先进后出"},{"label":"C","content":"随机访问"},{"label":"D","content":"键值对存储"}]', 'B', '栈(Stack)是一种后进先出(LIFO)的数据结构，常用操作：push(入栈)、pop(出栈)、peek(查看栈顶)。', 1, '栈,基础'),

(3, 1, '二叉查找树(BST)的中序遍历结果是？', '[{"label":"A","content":"无序序列"},{"label":"B","content":"递增有序序列"},{"label":"C","content":"递减有序序列"},{"label":"D","content":"层序遍历序列"}]', 'B', '二叉查找树（左子树<根<右子树）的中序遍历（左根右）结果是递增有序序列，这是BST的重要性质。', 1, '二叉树,BST,基础'),

(3, 1, '数组和链表的主要区别是什么？', '[{"label":"A","content":"数组随机访问快，链表插入删除快"},{"label":"B","content":"数组插入快，链表查找快"},{"label":"C","content":"没有区别"},{"label":"D","content":"数组只能在栈上分配"}]', 'A', '数组支持O(1)随机访问，但插入/删除需要O(n)移动元素。链表查找需要O(n)，但插入/删除只需要O(1)（已知位置时）。', 1, '数组,链表,基础'),

(3, 1, '快速排序的平均时间复杂度是多少？', '[{"label":"A","content":"O(n)"},{"label":"B","content":"O(n log n)"},{"label":"C","content":"O(n²)"},{"label":"D","content":"O(log n)"}]', 'B', '快速排序的平均时间复杂度为O(n log n)，最坏情况（已排序数组）为O(n²)。空间复杂度O(log n)（递归栈）。', 2, '排序,复杂度'),

(3, 3, '散列表(Hash Table)的平均查询时间复杂度为O(n)。', NULL, '错误', '哈希表的平均查询、插入、删除时间复杂度均为O(1)，最坏情况（大量冲突）为O(n)。', 1, '哈希表,复杂度'),

(3, 1, '以下哪个数据结构最适合实现浏览器的前进/后退功能？', '[{"label":"A","content":"队列"},{"label":"B","content":"双栈"},{"label":"C","content":"堆"},{"label":"D","content":"优先队列"}]', 'B', '使用两个栈（X栈存后退页面，Y栈存前进页面）可以高效实现浏览器的前进后退功能。这是典型的双栈模式应用。', 2, '栈,应用'),

(3, 2, '以下哪些是平衡二叉树？', '[{"label":"A","content":"AVL树"},{"label":"B","content":"红黑树"},{"label":"C","content":"普通BST"},{"label":"D","content":"B-Tree"}]', 'A,B,D', 'AVL树、红黑树和B-Tree都是自平衡的树形结构。普通BST（二叉查找树）在最坏情况下可能退化为链表（高度为n）。', 2, '平衡树'),

(3, 4, '广度优先搜索(BFS)通常使用____数据结构实现，深度优先搜索(DFS)通常使用____数据结构实现。', NULL, '队列,栈', 'BFS使用队列（先进先出）逐层遍历；DFS使用栈（或递归）深入遍历。这是图遍历的两个基本算法。', 1, 'BFS,DFS,基础'),

-- ==================== 算法 (6题) ====================
(4, 1, '动态规划算法的核心思想是什么？', '[{"label":"A","content":"贪心选择"},{"label":"B","content":"分治+记忆化"},{"label":"C","content":"穷举所有可能"},{"label":"D","content":"随机搜索"}]', 'B', '动态规划将问题分解为重叠子问题，通过记忆化（缓存子问题解）避免重复计算。核心：最优子结构 + 重叠子问题。', 2, 'DP,动态规划'),

(4, 3, '贪心算法总是能得到全局最优解。', NULL, '错误', '贪心算法每一步做当前最优选择，但不保证全局最优。只有当问题具有贪心选择性质时（如最小生成树），贪心才得最优解。', 1, '贪心,基础'),

(4, 1, '二分查找的前提条件是什么？', '[{"label":"A","content":"数据必须有序"},{"label":"B","content":"数据必须是整数"},{"label":"C","content":"数据必须在数组中"},{"label":"D","content":"数据量必须大于100"}]', 'A', '二分查找要求数据已经排序（有序），每次将查找范围缩小一半，时间复杂度O(log n)。', 1, '二分查找,基础'),

(4, 2, '以下哪些算法的时间复杂度是O(n log n)？', '[{"label":"A","content":"归并排序"},{"label":"B","content":"堆排序"},{"label":"C","content":"冒泡排序"},{"label":"D","content":"快速排序(平均)"}]', 'A,B,D', '归并排序O(n log n)稳定，堆排序O(n log n)不稳定，快速排序平均O(n log n)。冒泡排序为O(n²)。', 1, '排序,复杂度'),

(4, 1, 'LRU缓存淘汰策略是指什么？', '[{"label":"A","content":"淘汰最先进入的"},{"label":"B","content":"淘汰最近最少使用的"},{"label":"C","content":"淘汰最大的"},{"label":"D","content":"随机淘汰"}]', 'B', 'LRU（Least Recently Used）淘汰最近最少使用的数据。通常用HashMap+双向链表实现，O(1)时间操作。', 2, 'LRU,缓存'),

(4, 1, '递归函数必须包含什么？', '[{"label":"A","content":"循环语句"},{"label":"B","content":"终止条件(基线条件)"},{"label":"C","content":"全局变量"},{"label":"D","content":"try-catch"}]', 'B', '递归函数必须包含终止条件（基线条件/Base case），否则会导致无限递归、栈溢出。', 1, '递归,基础'),

-- ==================== 数据库 (8题) ====================
(5, 1, 'SQL中，以下哪个关键字用于去重查询？', '[{"label":"A","content":"UNIQUE"},{"label":"B","content":"DISTINCT"},{"label":"C","content":"DIFFERENT"},{"label":"D","content":"FILTER"}]', 'B', 'SELECT DISTINCT column FROM table 可以去除查询结果中的重复行。UNIQUE是建表时约束列值唯一的。', 1, 'SQL,基础'),

(5, 1, '以下哪种索引在MySQL InnoDB中是聚簇索引？', '[{"label":"A","content":"普通索引"},{"label":"B","content":"唯一索引"},{"label":"C","content":"主键索引"},{"label":"D","content":"全文索引"}]', 'C', 'InnoDB中主键索引是聚簇索引，数据按照主键顺序物理存储。其他索引（二级索引）叶子节点存储主键值，需要回表查询。', 2, '索引,InnoDB'),

(5, 1, '事务的ACID特性中，I代表什么？', '[{"label":"A","content":"Integrity"},{"label":"B","content":"Isolation"},{"label":"C","content":"Index"},{"label":"D","content":"Insert"}]', 'B', 'ACID：Atomicity(原子性)、Consistency(一致性)、Isolation(隔离性)、Durability(持久性)。Isolation确保并发事务互不干扰。', 1, '事务,ACID,基础'),

(5, 1, 'LEFT JOIN和INNER JOIN的区别是什么？', '[{"label":"A","content":"没有区别"},{"label":"B","content":"LEFT JOIN保左表全部行，INNER JOIN只保留匹配行"},{"label":"C","content":"INNER JOIN保右表全部行"},{"label":"D","content":"LEFT JOIN更快"}]', 'B', 'LEFT JOIN返回左表所有行（右表无匹配填NULL），INNER JOIN只返回两表都有匹配的行。', 1, 'JOIN,SQL,基础'),

(5, 3, 'MySQL中，VARCHAR类型的字段可以存储的最大字符数是65535。', NULL, '正确', 'MySQL 5.0.3+中VARCHAR最大可存储65535字节（注意是字节不是字符，具体字符数取决于字符集，utf8mb4下最多约16383个字符）。', 2, 'MySQL,VARCHAR'),

(5, 2, '以下哪些是数据库事务的隔离级别？', '[{"label":"A","content":"READ UNCOMMITTED"},{"label":"B","content":"READ COMMITTED"},{"label":"C","content":"REPEATABLE READ"},{"label":"D","content":"SERIALIZABLE"}]', 'A,B,C,D', 'SQL标准定义了四个隔离级别：读未提交、读已提交、可重复读（MySQL默认）、串行化。隔离级别越高，并发性能越低。', 2, '事务,隔离级别'),

(5, 1, '什么情况下索引会失效？', '[{"label":"A","content":"使用LIKE %keyword%"},{"label":"B","content":"WHERE中使用函数包围列"},{"label":"C","content":"使用OR连接不同列的条件"},{"label":"D","content":"以上都是"}]', 'D', '索引失效的常见场景：like以%开头、对列使用函数或计算、OR连接非索引列、隐式类型转换、联合索引不满足最左前缀等。', 3, '索引优化,深入'),

(5, 4, 'MySQL中，InnoDB引擎默认的数据页大小是____KB。', NULL, '16', 'InnoDB默认数据页大小为16KB，通过innodb_page_size参数配置。每一页存储若干行记录，是磁盘IO的最小单位。', 3, 'MySQL,深入'),

-- ==================== Java (8题) ====================
(6, 1, 'Java中，String和StringBuilder的主要区别是什么？', '[{"label":"A","content":"String可变，StringBuilder不可变"},{"label":"B","content":"String不可变，StringBuilder可变"},{"label":"C","content":"没有区别"},{"label":"D","content":"StringBuilder线程安全"}]', 'B', 'String是不可变对象，每次修改都会创建新对象（开销大）。StringBuilder可变，适合频繁拼接字符串的场景（非线程安全）。StringBuffer线程安全。', 1, 'String,基础'),

(6, 1, 'HashMap的底层数据结构是什么？', '[{"label":"A","content":"数组"},{"label":"B","content":"数组+链表+红黑树"},{"label":"C","content":"链表"},{"label":"D","content":"二叉树"}]', 'B', 'Java 8中HashMap采用数组+链表+红黑树。当链表长度>8且数组长度>=64时，链表转为红黑树，提高查询效率（O(n) → O(log n)）。', 2, 'HashMap,集合'),

(6, 3, 'Java中，一个类可以实现多个接口，也可以继承多个父类。', NULL, '错误', 'Java中类只能单继承（extends一个父类），但可以实现多个接口（implements多个interface）。这是Java为了避免菱形继承问题的设计。', 1, '继承,接口,基础'),

(6, 1, '以下哪个不是Java的基本数据类型？', '[{"label":"A","content":"int"},{"label":"B","content":"String"},{"label":"C","content":"boolean"},{"label":"D","content":"double"}]', 'B', 'Java的8种基本类型：byte, short, int, long, float, double, char, boolean。String是引用类型，不是基本类型。', 1, '基本类型,基础'),

(6, 1, 'JVM中垃圾回收的主要区域是哪里？', '[{"label":"A","content":"栈内存"},{"label":"B","content":"堆内存"},{"label":"C","content":"方法区"},{"label":"D","content":"程序计数器"}]', 'B', 'GC主要回收堆内存中的对象。栈内存随方法的进入和退出自动分配和释放，不需要GC。方法区/元空间也会回收（类卸载）。', 2, 'JVM,GC'),

(6, 2, '以下哪些是Java线程安全的集合类？', '[{"label":"A","content":"ConcurrentHashMap"},{"label":"B","content":"CopyOnWriteArrayList"},{"label":"C","content":"HashMap"},{"label":"D","content":"ArrayList"}]', 'A,B', 'ConcurrentHashMap（分段锁/CAS）和CopyOnWriteArrayList（写时复制）是线程安全的。HashMap和ArrayList非线程安全。', 2, '并发,集合'),

(6, 1, 'Spring中@Autowired和@Resource的区别？', '[{"label":"A","content":"@Autowired按类型注入，@Resource按名称注入"},{"label":"B","content":"@Autowired按名称注入，@Resource按类型注入"},{"label":"C","content":"没有区别"},{"label":"D","content":"@Resource是Spring专属"}]', 'A', '@Autowired是Spring注解，默认按类型(byType)注入。@Resource是JSR-250(Jakarta)标准，默认按名称(byName)注入，找不到名称再按类型。', 2, 'Spring,DI'),

(6, 4, 'Java中____关键字用于创建对象实例，____关键字用于声明常量。', NULL, 'new,final', 'new用于在堆上创建对象实例并调用构造方法。final可修饰类（不可继承）、方法（不可重写）、变量（不可修改）。', 1, '关键字,基础'),

-- ==================== 设计模式 (4题) ====================
(7, 1, '单例模式中，哪种实现方式能防止反射攻击？', '[{"label":"A","content":"饿汉式"},{"label":"B","content":"懒汉式"},{"label":"C","content":"枚举式"},{"label":"D","content":"静态内部类"}]', 'C', '枚举实现的单例模式天然防止反射攻击和序列化破坏。Java保证枚举实例在JVM中唯一，且反射API无法创建枚举实例。', 3, '单例,枚举,深入'),

(7, 1, '工厂模式的主要目的是什么？', '[{"label":"A","content":"提高代码执行效率"},{"label":"B","content":"将对象的创建和使用分离"},{"label":"C","content":"减少代码行数"},{"label":"D","content":"美化代码结构"}]', 'B', '工厂模式将对象的创建过程封装起来，使得调用者无需知道具体创建细节。遵循开闭原则，新增产品只需扩展工厂，无需修改调用代码。', 1, '工厂模式,基础'),

(7, 3, '装饰器模式和代理模式在结构上完全不同。', NULL, '错误', '装饰器模式和代理模式在结构上相似（都包含目标对象的引用），但目的不同：装饰器增强功能，代理控制访问。两者类图相似但语义不同。', 2, '装饰器,代理'),

(7, 1, '观察者模式中，以下哪个角色负责通知？', '[{"label":"A","content":"Observer"},{"label":"B","content":"Subject(被观察者)"},{"label":"C","content":"Client"},{"label":"D","content":"Factory"}]', 'B', '观察者模式：Subject（被观察者/主题）维护观察者列表，状态变化时主动通知所有观察者。Java中已废弃的Observer/Observable和EventBus都是此模式。', 1, '观察者,基础');

-- ==================== 计算机组成原理 (补充) ====================
INSERT INTO question (category_id, type, title, options, answer, analysis, difficulty, tags) VALUES

(8, 1, 'CPU中，ALU的全称是什么？', '[{"label":"A","content":"算术逻辑单元"},{"label":"B","content":"控制单元"},{"label":"C","content":"内存管理单元"},{"label":"D","content":"图形处理单元"}]', 'A', 'ALU（Arithmetic Logic Unit，算术逻辑单元）是CPU的核心部件，负责执行算术运算和逻辑运算。', 1, 'CPU,基础'),

(8, 3, '32位操作系统最多只能使用4GB内存。', NULL, '正确', '32位地址总线最多寻址2^32=4GB内存空间。但通过PAE（物理地址扩展）技术可以支持超过4GB物理内存（单个进程仍受4GB虚拟地址空间限制）。', 2, '内存,32位');

-- 更新question表的选项JSON（清理NULL）
UPDATE question SET options = NULL WHERE options IS NULL OR type IN (3, 4);
