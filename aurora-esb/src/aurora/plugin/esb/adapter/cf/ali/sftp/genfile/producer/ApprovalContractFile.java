package aurora.plugin.esb.adapter.cf.ali.sftp.genfile.producer;

import org.apache.camel.Exchange;
import org.apache.camel.Message;

public class ApprovalContractFile {

	private int batchNo = 1;

	public void genFile(Exchange exchange) {

		// camelfilenameconsumed:CFCar_AUTOFI_PAYEE_INFO_20150202_1.txt
		// camelfileparent:/Users/shiliyan/Desktop/esb/download/CFCar
		// camelfilelength:138
		// camelfilerelativepath:CFCar_AUTOFI_PAYEE_INFO_20150202_1.txt
		// camelfilenameonly:CFCar_AUTOFI_PAYEE_INFO_20150202_1.txt
		// camelfileabsolute:true
		// camelfilelastmodified:1432703619000
		// camelfilename:CFCar_AUTOFI_PAYEE_INFO_20150202_1.txt
		// breadcrumbid:ID-itpc118-corp-cf-finance-com-63988-1432703582588-0-41
		// camelfileabsolutepath:/Users/shiliyan/Desktop/esb/download/CFCar/CFCar_AUTOFI_PAYEE_INFO_20150202_1.txt
		// camelfilepath:/Users/shiliyan/Desktop/esb/download/CFCar/CFCar_AUTOFI_PAYEE_INFO_20150202_1.txt
		// version:1.0|count:1|isLast:NO
		// applyNo|name|address|phone
		// 94012014070100039589S|杭州XX经销商|浙江省杭州市西湖区学院路999号|057188888888
		// exchange.getIn().s
//		isLast
		batchNo++;
		Message out = exchange.getOut();
		out.setHeader(
				"camelfilename",
				"/CFCar/AUTOFI_APPROVAL_CONTRACT/20150601/CFCar_AUTOFI_APPROVAL_CONTRACT_20150202_"
						+ batchNo + ".txt");
		// out.setHeader("camelfileabsolute",
		// "false");
		// out.setHeader(
		// "camelfilenameconsumed",
		// "/CFCar/AUTOFI_APPROVAL_CONTRACT/20150601/CFCar_AUTOFI_APPROVAL_CONTRACT_20150202_2.txt");
		// out.setHeader("camelfilepath",
		// "/AUTOFI_APPROVAL_CONTRACT/20150601/");
		// out.setHeader(
		// "camelfileabsolutepath",
		// "/Users/shiliyan/Desktop/esb/upload/CFCar/AUTOFI_APPROVAL_CONTRACT/20150601/CFCar_AUTOFI_APPROVAL_CONTRACT_20150202_2.txt");
		out.setBody("version:1.0|count:3|isLast:NO\napplyNo|allowLoan|failReason|reasonType|applyAmount|contractNo\n94012014070100039587S|YES|||1000|AAAAAAA");

		out.setBody(null);
		System.out.println("???");

		// return exchange;
	}

}