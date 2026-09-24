<template>
	<!--
		隐私政策（静态页）
		⚠️ 本文本为【通用模板】，按 2026 年微信小程序个人信息保护合规要求整理，**不是法律意见**。
		   正式上线前必须由「运营主体」与法务确认后定稿；「信息收集清单」「第三方SDK清单」须与代码实际行为逐项核对。
		   把 data() 里的 showTemplateNotice 改为 false 即可隐藏顶部「模板」提示条。
	-->
	<view class="agreement-page">
		<view class="tpl-notice" v-if="showTemplateNotice">
			<text class="tpl-notice-txt">本文本为通用模板版本，正式发布前须经运营主体与法务确认，并逐项核对收集清单</text>
		</view>

		<view class="doc">
			<view class="doc-title">{{ brandName }}隐私政策</view>
			<view class="doc-meta">生效日期：{{ effectiveDate }}　|　版本号：v1.0</view>

			<view class="sec">
				<view class="sec-h">引言</view>
				<view class="sec-p">{{ operator }}（以下简称"我们"）深知个人信息对您的重要性，并会尽全力保护您的个人信息安全可靠。我们致力于维持您对我们的信任，恪守以下原则保护您的个人信息：权责一致原则、目的明确原则、选择同意原则、最小必要原则、确保安全原则、主体参与原则、公开透明原则。</view>
				<view class="sec-p">请在使用{{ brandName }}小程序（以下简称"本小程序"）前，仔细阅读并了解本隐私政策。本政策中与您的个人信息权益存在重大关系的条款，我们将采用加粗形式提示您注意。</view>
			</view>

			<view class="sec">
				<view class="sec-h">一、我们如何收集和使用您的个人信息</view>
				<view class="sec-p">我们仅会出于本政策所述的以下目的，收集和使用您的个人信息。若我们要将您的个人信息用于本政策未载明的其他用途，或将基于特定目的收集的信息用于其他目的，我们将再次征得您的同意。</view>

				<view class="tbl">
					<view class="tbl-head">
						<view class="tbl-h c1">业务场景</view>
						<view class="tbl-h c2">收集的信息</view>
						<view class="tbl-h c3">目的</view>
					</view>
					<view class="tbl-row">
						<view class="tbl-c c1">注册 / 登录</view>
						<view class="tbl-c c2">微信 OpenID、手机号码</view>
						<view class="tbl-c c3">创建会员账号、识别您的身份</view>
					</view>
					<view class="tbl-row">
						<view class="tbl-c c1">完善会员资料</view>
						<view class="tbl-c c2">微信头像、昵称</view>
						<view class="tbl-c c3">展示会员头像与昵称（可选，拒绝不影响下单）</view>
					</view>
					<view class="tbl-row">
						<view class="tbl-c c1">门店选择</view>
						<view class="tbl-c c2">地理位置（经纬度）</view>
						<view class="tbl-c c3">计算您与门店的距离、为您推荐就近门店</view>
					</view>
					<view class="tbl-row">
						<view class="tbl-c c1">堂食点餐</view>
						<view class="tbl-c c2">桌台号、区域</view>
						<view class="tbl-c c3">识别就餐桌位、安排出餐与送餐</view>
					</view>
					<view class="tbl-row">
						<view class="tbl-c c1">下单与支付</view>
						<view class="tbl-c c2">订单信息（商品、数量、金额、门店、时间）</view>
						<view class="tbl-c c3">生成并履行订单、处理支付与退款、售后</view>
					</view>
					<view class="tbl-row">
						<view class="tbl-c c1">会员权益</view>
						<view class="tbl-c c2">积分、优惠券、储值余额记录</view>
						<view class="tbl-c c3">发放与核销会员权益</view>
					</view>
					<view class="tbl-row">
						<view class="tbl-c c1">实名 / 身份认证</view>
						<view class="tbl-c c2">真实姓名、身份证件正反面照片（人像面含人脸图像）；学生 / 教师认证另需学生证或教师资格证照片</view>
						<view class="tbl-c c3">核验您的真实身份与在校 / 在职身份（仅您主动发起时收集，需单独同意）</view>
					</view>
					<view class="tbl-row">
						<view class="tbl-c c1">安全保障</view>
						<view class="tbl-c c2">设备型号、操作系统、网络类型、登录日志</view>
						<view class="tbl-c c3">预防、发现和处置账号安全风险与异常交易</view>
					</view>
				</view>

				<view class="sec-p">1.1 <text class="strong">敏感个人信息提示</text>：手机号码，以及您主动发起「实名认证 / 身份认证」时提交的身份证件正反面照片（<text class="strong">人像面包含您的人脸图像</text>）、学生证或教师资格证照片，属于您的敏感个人信息；其中人脸图像属于生物识别信息。手机号码用于完成账号注册与登录、订单通知与售后联系，如您不提供手机号码，将无法使用需要账号登录的功能（如下单、查看订单），但不影响您浏览商品与门店信息。证件类信息的处理规则与单独同意机制详见第 1.5、1.6 条。</view>
				<view class="sec-p">1.2 您可选择是否授权微信头像与昵称。若您拒绝授权，我们不会获取该等信息，您仍可通过手机号验证码方式完成登录，头像将展示为默认图片。</view>
				<view class="sec-p">1.3 您可选择是否授权地理位置。若您拒绝授权，我们将无法计算您与门店的距离，您仍可手动浏览并选择门店。</view>
				<view class="sec-p">1.4 为实现上述功能，我们需要您开启相应权限。您可在微信"设置 — 隐私 — 授权管理"或系统设置中随时关闭相关授权。关闭后，对应功能将不可用，但不影响其他功能的正常使用。</view>
				<view class="sec-p">1.5 <text class="strong">关于身份证件照片（单独同意）</text>：仅当您主动发起「实名认证 / 身份认证」时，我们才会收集您的真实姓名与身份证件正反面照片，用于人工核验您的真实身份。此类信息属于敏感个人信息，我们将在取得您的<text class="strong">单独同意</text>后处理——认证页面设有独立的勾选框与独立的《实名认证个人信息处理告知书》，<text class="strong">不并入</text>《用户协议》《隐私政策》等一揽子同意，也不作为您使用点餐、下单等基础服务的前提条件。我们对身份证件照片<text class="strong">仅做人工核验</text>，不使用人脸识别算法对其进行自动化处理，亦不将其用于广告、营销、用户画像或任何与身份核验无关的目的。我们不会收集您的指纹、声纹等其他生物识别信息。</view>
				<view class="sec-p">1.6 <text class="strong">处理该等信息的必要性及对您个人权益的影响</text>：真实身份核验只能以核对证件原件或证件图像的方式实现，因此收集身份证件照片具有充分必要性；如您拒绝提供，我们将无法为您完成实名 / 身份认证，但不影响您浏览商品、门店信息与到店消费等基础功能。身份证件照片一旦泄露或被滥用，可能对您的人身、财产安全造成不利影响，为此我们采取了传输加密、访问权限最小化、审核操作留痕、限时删除等措施以降低风险。您可以随时通过第八条的联系方式撤回前述单独同意；撤回后我们将停止处理并删除相应材料，但不影响撤回前基于该同意已进行的处理活动。除上述已告知的目的外，我们不会收集与所提供服务无关的个人信息。</view>
			</view>

			<view class="sec">
				<view class="sec-h">二、我们如何使用 Cookie 及同类技术</view>
				<view class="sec-p">2.1 为保障本小程序正常运转、简化您重复登录的步骤，我们会在您的本地设备上存储登录凭证（Token）等数据。上述数据仅用于身份校验与会话维持，不包含您的支付密码等敏感信息。</view>
				<view class="sec-p">2.2 您可通过清除小程序缓存的方式清除上述本地存储数据。清除后需要重新登录。</view>
			</view>

			<view class="sec">
				<view class="sec-h">三、我们如何共享、转让、公开披露您的个人信息</view>
				<view class="sec-p">3.1 我们不会向任何第三方出售您的个人信息。</view>
				<view class="sec-p">3.2 为实现本小程序的基础功能，我们可能需要与以下第三方共享必要信息。我们会与其签署严格的数据保护协议，要求其按照本政策及法律法规要求处理个人信息：</view>

				<view class="tbl">
					<view class="tbl-head">
						<view class="tbl-h c1">第三方名称</view>
						<view class="tbl-h c2">涉及信息</view>
						<view class="tbl-h c3">使用目的</view>
					</view>
					<view class="tbl-row">
						<view class="tbl-c c1">微信小程序平台（腾讯）</view>
						<view class="tbl-c c2">微信 OpenID、头像昵称（经您授权）</view>
						<view class="tbl-c c3">提供登录能力与小程序运行环境</view>
					</view>
					<view class="tbl-row">
						<view class="tbl-c c1">微信支付（腾讯）</view>
						<view class="tbl-c c2">订单金额、商户订单号</view>
						<view class="tbl-c c3">完成订单支付与退款</view>
					</view>
					<view class="tbl-row">
						<view class="tbl-c c1">腾讯云（云托管 / 数据库）</view>
						<view class="tbl-c c2">本政策第一款所列全部信息</view>
						<view class="tbl-c c3">提供服务器托管与数据存储服务</view>
					</view>
					<view class="tbl-row">
						<view class="tbl-c c1">阿里云（对象存储 OSS）</view>
						<view class="tbl-c c2">您上传的图片，包括头像、认证材料照片等</view>
						<view class="tbl-c c3">存储图片文件，供页面展示与人工审核读取</view>
					</view>
				</view>

				<view class="sec-p">3.3 除下列情形外，我们不会将您的个人信息转让给任何第三方：（1）取得您的明确同意；（2）因合并、分立、解散、被收购等原因需要转移的，我们会要求新的持有方继续受本政策约束。</view>
				<view class="sec-p">3.4 我们仅会在下列情形公开披露您的个人信息：（1）取得您的明确同意；（2）根据法律法规、法律程序、诉讼或政府主管部门强制性要求。</view>
			</view>

			<view class="sec">
				<view class="sec-h">四、我们如何保护您的个人信息</view>
				<view class="sec-p">4.1 我们已使用符合业界标准的安全防护措施保护您提供的个人信息，防止数据遭到未经授权的访问、公开披露、使用、修改、损坏或丢失。我们采用传输层加密（HTTPS）保护数据传输，并对存储的敏感字段采取加密或脱敏处理。</view>
				<view class="sec-p">4.2 我们仅允许有必要知晓的员工访问个人信息，并为此设置了严格的访问权限控制与审计机制。</view>
				<view class="sec-p">4.3 我们会在法律法规允许的期限内保留您的个人信息，超出期限后将予以删除或匿名化处理。一般而言：账号信息在账号存续期间保留；交易信息自订单完成之日起保留不少于 3 年（法律法规另有规定的从其规定）。</view>
				<view class="sec-p">4.4 如不幸发生个人信息安全事件，我们将按法律法规要求，及时以推送通知、公告等方式告知您：事件基本情况、我们已采取的处置措施、您可自主防范的建议等，并向监管部门报告。</view>
				<view class="sec-p">4.5 <text class="strong">实名 / 身份认证材料的保存期限</text>：身份证件、学生证、教师资格证等照片自审核结论作出之日起保存 {{ identityKeepDays }} 日，期满后我们即予删除或匿名化处理，认证记录中仅保留审核结论等必要痕迹，不再保留证件图像。保存期间如您撤回单独同意或申请注销账号，我们将提前删除相应材料。您也可通过第八条的联系方式随时要求提前删除。</view>
			</view>

			<view class="sec">
				<view class="sec-h">五、您的权利</view>
				<view class="sec-p">按照中国相关法律法规，我们保障您对自己的个人信息行使以下权利：</view>
				<view class="sec-p">5.1 <text class="strong">查阅与更正</text>：您可在本小程序"我的 — 个人资料"中查阅、更正您的头像、昵称等信息。</view>
				<view class="sec-p">5.2 <text class="strong">删除</text>：在以下情形下，您可要求我们删除您的个人信息：（1）处理目的已实现、无法实现或不再必要；（2）我们停止提供产品或服务；（3）您撤回同意；（4）我们违反法律法规或与您的约定处理个人信息。</view>
				<view class="sec-p">5.3 <text class="strong">撤回同意</text>：您可通过关闭微信授权（"设置 — 隐私 — 授权管理"）或通过本政策第八条的联系方式，撤回您对个人信息处理的同意。撤回同意不影响撤回前基于同意已进行的处理活动。</view>
				<view class="sec-p">5.4 <text class="strong">注销账号</text>：您可通过本政策第八条的联系方式申请注销账号。账号注销后，我们将删除或匿名化您的个人信息，法律法规另有规定的除外。请注意，注销后您在本小程序的会员等级、积分、优惠券等权益将一并失效且无法恢复。</view>
				<view class="sec-p">5.5 <text class="strong">投诉举报</text>：如您认为我们的个人信息处理行为损害了您的合法权益，可通过本政策第八条的联系方式投诉，我们将在 15 个工作日内答复。您也可向网信、电信、公安等主管部门反映。</view>
				<view class="sec-p">5.6 为保障安全，在您行使上述权利时，我们可能需要您提供书面请求或验证您的身份（如通过注册手机号接收验证码）。</view>
				<view class="sec-p">5.7 我们将在收到您的请求并核验身份后 15 个工作日内予以答复。</view>
			</view>

			<view class="sec">
				<view class="sec-h">六、未成年人保护</view>
				<view class="sec-p">6.1 本小程序主要面向成年人。我们根据相关法律法规的规定保护未成年人的个人信息。</view>
				<view class="sec-p">6.2 若您是未满 18 周岁的未成年人，请在您的父母或其他监护人指导下阅读本政策，并在其同意后使用本小程序服务、提供个人信息。</view>
				<view class="sec-p">6.3 若您是未成年人的父母或其他监护人，对您所监护的未成年人的个人信息有疑问，可通过本政策第八条的联系方式与我们联系。</view>
				<view class="sec-p">6.4 <text class="strong">本小程序销售的酒类商品仅在法律法规允许的范围内提供，我们不向未成年人销售酒类商品。</text></view>
			</view>

			<view class="sec">
				<view class="sec-h">七、本政策的更新</view>
				<view class="sec-p">7.1 我们可能适时修订本政策的条款。未经您明确同意，我们不会削减您按照本政策所应享有的权利。</view>
				<view class="sec-p">7.2 对于重大变更（如服务模式发生重大变化、个人信息使用目的或类型发生实质变化、对外共享对象发生变化等），我们会在小程序内以弹窗、公告或推送通知等显著方式告知您，并在必要时重新征得您的同意。</view>
				<view class="sec-p">7.3 本政策更新后，您继续使用本小程序服务，即表示您已充分阅读、理解并接受更新后的政策。</view>
			</view>

			<view class="sec">
				<view class="sec-h">八、如何联系我们</view>
				<view class="sec-p">如您对本隐私政策或您个人信息的处理有任何疑问、意见、投诉或举报，您可通过以下方式联系我们，我们将在 15 个工作日内答复：</view>
				<view class="sec-p">运营主体：{{ operator }}</view>
				<view class="sec-p">个人信息保护负责人邮箱：{{ privacyEmail }}</view>
				<view class="sec-p">客服电话：{{ contactPhone }}</view>
				<view class="sec-p">注册地址：{{ operatorAddress }}</view>
			</view>
		</view>

		<view class="doc-footer">{{ brandName }}　|　{{ operator }}</view>
	</view>
</template>

<script>
	// 保存期限等面向用户的合规数值统一从此处取，避免与认证页告知书文案漂移
	import memberConsent from '@/utils/member-consent';

	export default {
		data() {
			return {
				// 模板提示条：正式发布前把这里改为 false（或由运营方确认后移除）
				showTemplateNotice: true,
				// ⚠️ 品牌名为暂定值，运营主体/联系方式均为占位符，须由运营方替换
				brandName: '湖里Lakes',
				operator: '【运营主体全称，待确认】',
				operatorAddress: '【注册地址，待确认】',
				privacyEmail: '【个人信息保护负责人邮箱，待确认】',
				contactPhone: '【客服电话，待确认】',
				effectiveDate: '【生效日期，待确认】',
				// 实名/身份认证材料保存期限（日）——见第 4.5 条
				// 数值唯一来源：utils/member-consent.js（与认证页告知书弹窗共用，避免两处文本漂移）
				// ⚠️ 该承诺目前无能力支撑（后端无删除机制 + mvp 下定时任务不启动），不得携本版本上线
				identityKeepDays: memberConsent.IDENTITY_KEEP_DAYS
			};
		},
		onShareAppMessage() {
			return {
				title: this.brandName + '隐私政策',
				path: '/pages/internal/agreement/privacy'
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

	.strong {
		font-weight: 600;
		color: #333333;
	}

	/* 收集清单 / 第三方清单表格 */
	.tbl {
		margin-top: 16rpx;
		border: 1rpx solid #ededed;
		border-radius: 8rpx;
		overflow: hidden;
	}

	.tbl-head {
		display: flex;
		background: #f5f5f5;
	}

	.tbl-h {
		font-size: 22rpx;
		font-weight: 600;
		color: #333333;
		padding: 14rpx 12rpx;
		line-height: 30rpx;
	}

	.tbl-row {
		display: flex;
		border-top: 1rpx solid #ededed;
	}

	.tbl-c {
		font-size: 22rpx;
		color: #666666;
		padding: 14rpx 12rpx;
		line-height: 32rpx;
	}

	.c1 {
		width: 26%;
	}

	.c2 {
		width: 36%;
	}

	.c3 {
		width: 38%;
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
