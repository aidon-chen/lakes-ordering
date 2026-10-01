<template>
	<loading-toast />
	<!--
		用户协议（静态页）
		⚠️ 本文本为【通用模板】，由开发方按 2026 年微信小程序合规要求整理，**不是法律意见**。
		   正式上线前必须由「运营主体」与法务确认后定稿，并将下方 OPERATOR / CONTACT 替换为真实信息。
		   把 data() 里的 showTemplateNotice 改为 false 即可隐藏顶部「模板」提示条。
	-->
	<view class="agreement-page">
		<view class="tpl-notice" v-if="showTemplateNotice">
			<text class="tpl-notice-txt">本文本为通用模板版本，正式发布前须经运营主体与法务确认</text>
		</view>

		<view class="doc">
			<view class="doc-title">{{ brandName }}用户服务协议</view>
			<view class="doc-meta">生效日期：{{ effectiveDate }}　|　版本号：v1.0</view>

			<view class="sec">
				<view class="sec-h">一、协议的接受与范围</view>
				<view class="sec-p">1.1 本协议是您与{{ operator }}（以下简称"我们"）之间就使用{{ brandName }}小程序（以下简称"本小程序"）及相关服务所订立的契约。</view>
				<view class="sec-p">1.2 您在注册、登录或使用本小程序服务前，应完整阅读本协议全部条款。您勾选"已阅读并同意"、完成注册或开始使用本小程序服务，即视为您已充分理解并同意本协议全部内容。</view>
				<view class="sec-p">1.3 若您不同意本协议任何条款，请立即停止注册或使用本小程序服务。</view>
				<view class="sec-p">1.4 您确认，在您开始使用本小程序服务时，您应为具有完全民事行为能力的自然人。若您不具备前述资格，请在监护人陪同下阅读本协议并决定是否使用本小程序服务。</view>
			</view>

			<view class="sec">
				<view class="sec-h">二、账号注册与安全</view>
				<view class="sec-p">2.1 您可通过微信授权登录或手机号验证码方式注册、登录本小程序。首次通过手机号验证码登录时，系统将自动为您创建会员账号，无需另行填写注册信息。</view>
				<view class="sec-p">2.2 您应保证注册及使用过程中提供的手机号等信息真实、准确、合法、有效，并在信息变更时及时更新。</view>
				<view class="sec-p">2.3 您的账号仅限您本人使用。因您主动泄露、出借账号，或您保管不善导致账号被他人使用所产生的损失，由您自行承担。</view>
				<view class="sec-p">2.4 如您发现账号存在异常登录、被盗用等情况，应立即通过本协议第十条的联系方式通知我们，我们将协助您处理。</view>
			</view>

			<view class="sec">
				<view class="sec-h">三、服务内容</view>
				<view class="sec-p">3.1 本小程序为您提供以下服务：门店浏览与选择、堂食扫码点餐、自提/外带点餐、商品（酒水等）浏览与购买、订单查询与管理、会员信息与权益管理等。</view>
				<view class="sec-p">3.2 堂食点餐服务中，您通过扫描桌台二维码进入点餐，系统将自动带入桌台信息。请您在下单前核对桌号，因桌号有误导致的送错、延误等，由您与我们协商解决。</view>
				<view class="sec-p">3.3 商品及服务的名称、价格、库存、规格等以您下单时本小程序页面展示的信息为准。因原料售罄、门店歇业等客观原因导致无法供应的，我们将及时通知您并为您办理退款。</view>
				<view class="sec-p">3.4 我们可能根据经营需要调整、暂停或终止部分服务内容，并将以本小程序公告等方式提前告知。</view>
			</view>

			<view class="sec">
				<view class="sec-h">四、下单、支付与退款</view>
				<view class="sec-p">4.1 您提交订单并完成支付后，订单即成立并生效。订单信息（含商品、数量、金额、门店、桌台/自提信息）可在"订单中心"查看。</view>
				<view class="sec-p">4.2 本小程序内所有支付均通过微信支付完成。您应确保支付账户合法有效，并妥善保管支付密码等验证信息。</view>
				<view class="sec-p">4.3 订单取消与退款按以下规则处理：未开始制作的订单，您可申请取消并全额退款；已开始制作的订单，我们有权视制作进度部分或全额拒绝退款；已交付的订单不支持退款。因商品质量问题产生的退款，不受上述限制。</view>
				<view class="sec-p">4.4 退款将按原支付路径退回，到账时间以微信支付及银行处理时效为准。使用优惠券、会员折扣的订单，按实际支付金额退款，优惠权益按原规则处理。</view>
			</view>

			<view class="sec">
				<view class="sec-h">五、会员权益与优惠规则</view>
				<view class="sec-p">5.1 会员等级、积分、优惠券、储值等权益的具体内容与获取、使用、有效期规则，以本小程序对应页面展示为准。</view>
				<view class="sec-p">5.2 会员权益仅限本人账号使用，不得转让、出售或用于任何营利性活动。如发现通过虚假交易、恶意刷单、批量注册等方式不当获取权益，我们有权取消相关权益并视情节限制账号功能。</view>
				<view class="sec-p">5.3 储值余额（如有）仅可用于本小程序内消费，不可提现、不可转让。法律法规另有规定的除外。</view>
			</view>

			<view class="sec">
				<view class="sec-h">六、用户行为规范</view>
				<view class="sec-p">6.1 您在使用本小程序服务时，应遵守中华人民共和国相关法律法规，不得从事下列行为：</view>
				<view class="sec-p">（1）发布、传输任何违法、有害、骚扰、诽谤、淫秽或其他令人不适的内容；</view>
				<view class="sec-p">（2）以任何方式干扰、破坏本小程序的正常运行，或非法侵入、攻击相关服务器与网络；</view>
				<view class="sec-p">（3）利用技术手段批量注册账号、恶意下单、虚假交易、套取优惠或积分；</view>
				<view class="sec-p">（4）未经授权收集、使用其他用户信息；</view>
				<view class="sec-p">（5）其他违反法律法规、公序良俗或本协议约定的行为。</view>
				<view class="sec-p">6.2 如您违反上述规范，我们有权视情节采取警告、删除内容、取消权益、限制或终止账号使用等措施，并保留追究法律责任的权利。</view>
			</view>

			<view class="sec">
				<view class="sec-h">七、知识产权</view>
				<view class="sec-p">7.1 本小程序内的文字、图片、标识、界面设计、程序代码等，其知识产权归我们或相应权利人所有。</view>
				<view class="sec-p">7.2 未经书面许可，您不得以任何方式复制、修改、传播、转载、汇编、反向工程或用于任何商业用途。</view>
				<view class="sec-p">7.3 您在本小程序发布的评价、图片等内容，您保证对其拥有合法权利；您授予我们在本小程序范围内免费使用该等内容的许可。</view>
			</view>

			<view class="sec">
				<view class="sec-h">八、免责与责任限制</view>
				<view class="sec-p">8.1 因不可抗力、网络故障、第三方服务（含微信平台、支付渠道、通信运营商等）中断或调整等非我们可控原因，导致服务中断、延迟或数据丢失的，我们不承担相应责任，但将尽力减少影响。</view>
				<view class="sec-p">8.2 因您自身原因（包括但不限于账号信息保管不当、设备故障、输入错误信息）造成的损失，由您自行承担。</view>
				<view class="sec-p">8.3 在法律允许的最大范围内，我们对您的赔偿责任总额，不超过您就相关订单实际支付的金额。</view>
			</view>

			<view class="sec">
				<view class="sec-h">九、协议的变更与终止</view>
				<view class="sec-p">9.1 我们有权根据法律法规变化及业务调整需要修改本协议，修改后的协议将在本小程序内公布。</view>
				<view class="sec-p">9.2 协议修改后，若您继续使用本小程序服务，即视为您接受修改后的协议；若您不同意，应停止使用并可申请注销账号。</view>
				<view class="sec-p">9.3 您可随时通过本协议第十条的联系方式申请注销账号。注销后，我们将按《隐私政策》约定删除或匿名化处理您的个人信息，法律法规另有规定的除外。</view>
			</view>

			<view class="sec">
				<view class="sec-h">十、法律适用、争议解决与联系方式</view>
				<view class="sec-p">10.1 本协议的订立、效力、解释、履行及争议解决均适用中华人民共和国法律。</view>
				<view class="sec-p">10.2 因本协议产生的争议，双方应友好协商解决；协商不成的，任何一方可向{{ operator }}所在地有管辖权的人民法院提起诉讼。</view>
				<view class="sec-p">10.3 如您对本协议有任何疑问、意见或投诉，可通过以下方式联系我们：</view>
				<view class="sec-p">运营主体：{{ operator }}</view>
				<view class="sec-p">客服邮箱：{{ contactEmail }}</view>
				<view class="sec-p">客服电话：{{ contactPhone }}</view>
			</view>

			<view class="sec">
				<view class="sec-h">附：关于第三方服务的说明</view>
				<view class="sec-p">本小程序运行于微信小程序平台，您对微信账号及微信支付的使用，同时受《微信软件许可及服务协议》《微信支付用户服务协议》及《腾讯隐私政策》约束。就本小程序内您个人信息的具体处理规则，请另行阅读《隐私政策》。</view>
			</view>
		</view>

		<view class="doc-footer">{{ brandName }}　|　{{ operator }}</view>
	</view>
</template>

<script>
	export default {
		data() {
			return {
				// 模板提示条：正式发布前把这里改为 false（或由运营方确认后移除）
				showTemplateNotice: true,
				// ⚠️ 品牌名为暂定值，运营主体/联系方式均为占位符，须由运营方替换
				brandName: '湖里Lakes',
				operator: '【运营主体全称，待确认】',
				contactEmail: '【客服邮箱，待确认】',
				contactPhone: '【客服电话，待确认】',
				effectiveDate: '【生效日期，待确认】'
			};
		},
		onShareAppMessage() {
			return {
				title: this.brandName + '用户服务协议',
				path: '/pages/internal/agreement/user'
			};
		}
	};
</script>

<style>
	.agreement-page {
		min-height: 100vh;
		background: #f7f7f7;
		padding-bottom: 60rpx;
	}

	.tpl-notice {
		background: #fff7e6;
		border-bottom: 1rpx solid #f0e0c0;
		padding: 16rpx 30rpx;
	}

	.tpl-notice-txt {
		font-size: 22rpx;
		color: #a06a12;
		line-height: 32rpx;
	}

	.doc {
		background: #ffffff;
		margin: 20rpx 20rpx 0 20rpx;
		border-radius: 16rpx;
		padding: 40rpx 30rpx 40rpx 30rpx;
	}

	.doc-title {
		font-size: 36rpx;
		font-weight: 600;
		color: #333333;
		text-align: center;
		line-height: 52rpx;
	}

	.doc-meta {
		font-size: 22rpx;
		color: #999999;
		text-align: center;
		margin-top: 16rpx;
		padding-bottom: 24rpx;
		border-bottom: 1rpx solid #ededed;
	}

	.sec {
		margin-top: 36rpx;
	}

	.sec-h {
		font-size: 28rpx;
		font-weight: 600;
		color: #333333;
		line-height: 44rpx;
		margin-bottom: 12rpx;
	}

	.sec-p {
		font-size: 26rpx;
		color: #666666;
		line-height: 46rpx;
		margin-top: 8rpx;
	}

	.doc-footer {
		font-size: 22rpx;
		color: #b0b0b0;
		text-align: center;
		margin-top: 40rpx;
		padding: 0 40rpx;
		line-height: 36rpx;
	}
</style>
