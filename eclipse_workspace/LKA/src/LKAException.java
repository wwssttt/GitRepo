public class LKAException extends Exception{
	
	/**
	 * serialVersionUID
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * 空构造函数
	 */
	public LKAException(){
		super();
	}
	
	/**
	 * 带字符串参数的构造函数
	 * @param message 需要抛出的信息说明
	 */
	public LKAException(String message){
		super(message);
		System.out.println("I am in LKAException: "+message);
	}
}
