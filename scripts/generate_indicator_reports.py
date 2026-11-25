"""依据用户指标字典生成独立模拟语料及答案，不修改业务数据或原始字典。"""
from pathlib import Path
import csv
import json
import random
import re
import zipfile
import openpyxl
from docx import Document
from docx.shared import Cm, Pt, RGBColor
from docx.oxml import OxmlElement
from docx.oxml.ns import qn

SOURCE = Path(r'C:\Users\Administrator\Desktop\竞赛相关\双碳金融科技创新大赛\转型金融评分模拟测试数据2023-2025\转型金融评分模拟数据2.0_指标字典.xlsx')
ROOT = Path(__file__).resolve().parents[1] / 'output' / '行业指标提取Word语料'
REPORTS = ROOT / '报告正文'
ANSWERS = ROOT / '评测答案'
rows = list(openpyxl.load_workbook(SOURCE, data_only=True).active.values)
common = [(i+2, r) for i, r in enumerate(rows[1:]) if r[0] == '全行业']
industries = list(dict.fromkeys(r[0] for r in rows[1:] if r[0] != '全行业'))

# 同义表述不附标准字段答案，供词频召回与语义相似度匹配；顺序与字典专项行一致。
ALIASES = {
 '钢铁行业': ['高炉炼铁每吨产品的标准煤消耗','转炉炼钢净工序能耗','电炉炼钢单位产品能耗','每吨粗钢对应的二氧化碳排放量','短流程电炉钢在全年钢产量中的比例','每吨粗钢的废钢投入量','高炉、转炉及焦炉副产煤气的回收利用比例','环境保护绩效等级'],
 '电力行业': ['清洁及可再生电源在总装机中的比重','清洁及可再生电源在总发电量中的比重','每千瓦时供电折合标准煤消耗','每兆瓦时发电的二氧化碳排放量','已完成灵活性改造的煤电机组比例','机组最低稳定运行出力占额定出力的比例','本年投入可再生电源建设的资金','环境保护绩效等级'],
 '基础化工行业': ['煤制合成氨每吨产品的综合能耗','石脑油裂解生产乙烯的吨产品能耗','每吨甲醇的综合能源消耗','每吨产品的二氧化碳排放量','绿电占全部用电的比例','绿氢与生物质等非化石原料的投入比例','工业用水重复循环利用比例','环境保护绩效等级'],
 '石油石化行业': ['按能量因数折算的炼油综合能耗','每加工一吨原油对应的二氧化碳排放量','每百万桶油当量对应的甲烷排放量','化工品及化工原料产量在产品中的比重','清洁和可再生能源在能源使用中的比例','可持续航空燃料与生物燃料的年度产量','环境保护绩效等级'],
 '建材行业': ['每吨水泥熟料综合能耗','每重量箱平板玻璃的综合能耗','每吨水泥产品对应的二氧化碳排放量','替代燃料贡献热量占燃料总热量的比例','替代原料在全部原料中的比例','环境保护绩效等级'],
 '有色金属行业': ['电解铝用电中绿电及可再生电力比例','每吨铝液的直流电耗','再生金属在金属供应中的比重','全年再生铝产出量','每吨铝产品的二氧化碳排放量','铝水不经重熔直接合金化的比例','环境保护绩效等级'],
 '煤炭行业': ['煤矿抽采瓦斯的利用比例','每吨原煤生产的综合能耗','矿井水资源综合利用比例','煤矸石和粉煤灰的综合利用比例','非煤业务在营业收入中的比例','期末新能源发电装机规模','环境保护绩效等级'],
 '航空机场行业': ['每完成一吨公里运输周转量的航空燃油消耗','每吨公里航空运输的二氧化碳排放量','每收入客公里的二氧化碳排放量','本年实际加注使用的可持续航空燃料数量','每位吞吐旅客对应的综合能耗','每位吞吐旅客对应的二氧化碳排放量','可再生能源占全部能源消费的比例','地面车辆与设备采用电驱动的比例','环境保护绩效等级']
}
SUBJECTS = ['示例恒岳钢铁集团','示例澄源电力集团','示例启衡化工集团','示例海川石化集团','示例青岚建材集团','示例铂源金属集团','示例北岭煤业集团','示例云程航空机场集团']
SCOPES = [
 '集团包括高炉—转炉联合生产基地和独立电弧炉基地，各工序能耗分别核算，不将不同工序直接相加。',
 '集团同时运营燃煤机组、风电和光伏电站；装机占比按期末容量统计，发电量占比按全年实际电量统计。',
 '集团下设煤制合成氨、石脑油裂解乙烯和甲醇三个生产单元；吨产品碳强度仅以甲醇单元作为示例边界，不跨产品混算。',
 '集团覆盖炼油、油气和生物燃料业务；炼油强度采用原油加工量，甲烷强度采用油当量，两种分母不能互换。',
 '集团同时设有熟料和玻璃生产基地；能耗按各自产品单位计量，碳排放强度仅指水泥基地，不包含玻璃产品。',
 '集团包括电解铝、再生铝和合金加工业务；吨铝碳强度以电解铝单元为边界，绿电比例采用电量口径。',
 '集团涵盖原煤开采、资源综合利用和新能源业务；生产能耗仅以原煤产量为分母，新能源规模采用期末装机口径。',
 '本虚构集团包括航空运输和机场运营两个板块。吨公里和收入客公里指标属于航空板块，旅客吞吐量指标属于机场板块，不能混用分母。'
]
GOV = [
 ('转型目标与承诺','董事会正式批准减排目标并对外作出承诺，明确责任部门和考核周期','尚未形成经董事会批准的转型目标，也没有对外发布减排承诺'),
 ('转型战略与路径规划','形成分阶段转型路线图，把节能改造、能源替代与投资安排纳入年度经营计划','尚未完成系统的转型战略和路线图，仅开展零散项目论证'),
 ('治理架构与激励','设置跨部门转型工作组，并将节能减排任务纳入管理人员绩效激励','未建立专门的转型治理组织，也未把相关目标纳入薪酬激励'),
 ('转型技术应用现状','已有转型技术在生产现场实际运行，运行记录由设备部门按月维护','转型技术仍处于方案研究阶段，报告期内未实际投入生产应用'),
 ('碳资产管理能力','建立碳资产台账，配备人员开展排放核算、配额管理和履约跟踪','尚未建立碳资产台账及专门管理机制，相关工作仅停留在数据收集'),
 ('风险管理与市场机遇','将碳价、能源价格和客户低碳采购要求纳入风险清单，同时评估低碳产品市场机会','尚未建立转型风险识别及市场机遇评估机制'),
 ('信息披露与透明度','公开年度转型进展，并披露统计边界、核算方法和主要不确定性','未公开年度转型进展，现有内部资料也未系统披露核算边界'),
 ('公正转型','安排岗位培训和转岗支持，建立员工沟通及申诉渠道，覆盖受改造影响的岗位','尚未制定面向受转型影响员工的培训、转岗或申诉支持方案'),
 ('环境合规','已完成报告期环境合规检查，相关排放满足适用要求且没有未整改的环境违法事项','报告期存在尚未完成整改的环境违规事项，不能认定已满足环境合规要求')
]

def clean(name):
    return name.split('(')[0].replace('经营质量_', '').replace('是否_', '')

def style_font(style, name, size):
    style.font.name = name
    style.font.size = Pt(size)
    style.element.get_or_add_rPr().rFonts.set(qn('w:eastAsia'), name)

def new_doc(title, company, year):
    doc = Document()
    # 清除运行环境默认模板的装饰边框，避免标题下出现蓝色横线。
    for style in doc.styles:
        for border in list(style.element.iter(qn('w:pBdr'))):
            border.getparent().remove(border)
    sec = doc.sections[0]
    sec.page_width, sec.page_height = Cm(21), Cm(29.7)
    sec.top_margin, sec.bottom_margin = Cm(2), Cm(2)
    sec.left_margin, sec.right_margin = Cm(2.2), Cm(2.2)
    for key in ['Normal', 'Title', 'Heading 1', 'Heading 2']:
        style_font(doc.styles[key], '宋体' if key == 'Normal' else '微软雅黑', 11 if key == 'Normal' else 19 if key == 'Title' else 14)
        doc.styles[key].font.color.rgb = RGBColor.from_string('000000')
    doc.styles['Normal'].paragraph_format.line_spacing = 1.25
    doc.styles['Normal'].paragraph_format.space_after = Pt(6)
    sec.header.paragraphs[0].text = '转型金融指标提取研究  /  模拟语料'
    footer = sec.footer.paragraphs[0]
    footer.alignment = 2
    footer.add_run('模拟材料 · 非真实企业披露    ')
    field = OxmlElement('w:fldSimple'); field.set(qn('w:instr'), 'PAGE'); footer._p.append(field)
    doc.add_paragraph(title, 'Title')
    doc.add_paragraph(f'{company}   |   {year}年1月1日至12月31日')
    doc.add_paragraph('本报告为指标提取算法实验编写的合成材料。企业、经营活动、评级和全部数值均为虚构，不构成真实披露、行业统计或授信依据。')
    return doc

def main():
    REPORTS.mkdir(parents=True, exist_ok=True); ANSWERS.mkdir(parents=True, exist_ok=True)
    answers = []
    for industry_index, industry in enumerate(industries):
        special = [(i+2, r) for i, r in enumerate(rows[1:]) if r[0] == industry]
        for variant, year in enumerate([2024, 2025]):
            rng = random.Random(20261112 + industry_index * 97 + variant * 13)
            company = SUBJECTS[industry_index]
            filename = f'{year}年{industry}报告.docx'
            folder = REPORTS / industry; folder.mkdir(exist_ok=True)
            doc = new_doc(filename[:-5], company, year)
            doc.add_paragraph('一 报告范围与转型治理', 'Heading 1')
            doc.add_paragraph(f'本报告记录{company}在{year}年的经营与低碳转型进展，数据截止日为12月31日。'+SCOPES[industry_index]+'下文数值均为当年实际完成值，未来安排另行标明。')
            def record(rownum, row, value, unit, phrase, evidence, section):
                answers.append(dict(file=f'报告正文/{industry}/{filename}', industry=industry, company=company, year=year,
                                    dictionary_row=rownum, indicator_type=row[1], indicator=row[3], dictionary_unit=row[4],
                                    value=value, unit=unit, mention=phrase, evidence=evidence, section=section, value_kind='actual'))
            for j, (name, positive, negative) in enumerate(GOV):
                value = 0 if (j == 4 and variant == 0) or (j == 7 and industry_index % 3 == 0) else 1
                phrase = (positive if value else negative)
                text = f'在{name}方面，集团{phrase}。' if variant == 0 else f'经年度管理复盘，集团{phrase}。相关结论以报告期末已执行的措施为准。'
                doc.add_paragraph(text)
                rownum, row = common[j]; record(rownum, row, value, '0/1', name if variant == 0 else phrase, text, '转型治理')
            doc.add_page_break()
            doc.add_paragraph('二 经营表现与资金保障', 'Heading 1')
            doc.add_paragraph('经营质量采用本模拟报告直接披露的0至1区间评分，不由以下三个财务比率反推。收入增长使用同口径上年度收入作分母，研发投入强度使用当年营业收入作分母。')
            vals = [round(rng.uniform(.58,.84),3), round(rng.uniform(4,13),2), round(rng.uniform(-2,12),2), round(rng.uniform(2,6),2)]
            mentions = ['企业经营质量','销售净利率','营业收入增长率','研发投入强度'] if variant == 0 else ['经营质量综合评价得分','净利润占销售收入的比例','营业收入较上年的增幅','研发支出占营业收入的比例']
            for k, (rownum, row) in enumerate(common[9:]):
                val, unit = vals[k], '分(0-1)' if k == 0 else '%'
                text = f'{year}年，{mentions[k]}为{val}{"分（满分1分）" if k == 0 else "%"}。'
                text += ['该分值仅表示本材料设定的经营质量评价结果，不代表贷款获批概率。','该比例反映扣除成本费用后的盈利状况，财务部门仍需跟踪原料价格波动。','负数表示同比下降，正数表示同比增长，不能把增幅直接当作收入金额。','研发项目主要围绕设备效率、过程监测和资源循环，投入比例不等于研发费用绝对额。'][k]
                doc.add_paragraph(text); record(rownum,row,val,unit,mentions[k],text,'经营表现')
            doc.add_paragraph('三 生产过程与能源效率', 'Heading 1')
            for j, (rownum, row) in enumerate(special):
                if j == 4:
                    doc.add_page_break(); doc.add_paragraph('四 资源利用与专项绩效', 'Heading 1')
                name, unit = row[3], row[4]
                mention = clean(name) if variant == 0 else ALIASES[industry][j]
                if name == '环保绩效评级':
                    value, unit = ('B级' if variant == 0 else 'A级'), '等级'
                else:
                    best, lag = float(row[6]), float(row[7])
                    value = round(lag + (best-lag)*rng.uniform(.35,.75), 3 if abs(lag-best)<2 else 2)
                    if name.startswith('水泥熟料'): value = round(rng.uniform(103,118),2)
                    if name.startswith('单位能量因数'): unit = 'kg oe/(t·能量因数)'
                text = f'根据{year}年全年运行汇总，{mention}为{value}{"" if unit == "等级" else " " + unit}。'
                if '转炉工序' in name and not name.startswith('高炉'):
                    text += '本项采用净能耗口径，扣除了回收煤气和蒸汽的折标煤量，因此负值并非漏填或识别错误。'
                elif '评级' in name:
                    text += '这是本模拟报告设定的年度环保绩效等级，仅用于测试评级文本抽取，并非真实主管部门认定。'
                elif '碳排放' in name or 'CO2' in name:
                    text += '本项为单位活动量对应的排放强度，不是排放总量；核算沿用前述业务边界，未抵扣外购碳信用。'
                elif '%' == unit:
                    text += '统计采用对应业务的全年实际量或期末规模，比例分母遵循该指标定义，不与其他产品的比例相加。'
                else:
                    text += '数据由对应业务单元汇总，按指标所列实物单位计量，未作百分制评分转换，也不是下一年度规划值。'
                doc.add_paragraph(text); record(rownum,row,value,unit,mention,text,'专项绩效')
            doc.add_paragraph('五 后续安排与口径说明', 'Heading 1')
            doc.add_paragraph(f'面向{year+1}年，集团计划将研发投入强度目标设为6.50%，该比例为未来预算目标，并非{year}年实绩。项目推进中将优先解决数据采集完整性、能源计量边界和跨部门复核问题，暂不据此判断转型贷款资格。')
            doc.add_paragraph('本报告没有披露的指标不应自动填零。正文中的管理措施需要结合肯定或否定语义判定；指标名称相近时仍需核对单位、时间和业务范围。')
            doc.save(folder / filename)
    # 答案独立存储，包含原文证据，可用于实体、数值、单位、年份四层评测。
    (ANSWERS/'指标标准答案.json').write_text(json.dumps(answers,ensure_ascii=False,indent=2),encoding='utf-8')
    with (ANSWERS/'指标标准答案.csv').open('w',encoding='utf-8-sig',newline='') as f:
        writer=csv.DictWriter(f,fieldnames=list(answers[0])); writer.writeheader(); writer.writerows(answers)
    readme = '''行业指标提取模拟语料说明

共8个行业、每行业2024及2025各1份Word报告，共16份。每份覆盖字典全部13项通用指标及该行业全部专项指标。所有企业与数据均为合成，不可用于证明真实行业表现。
2024版偏标准指标名称；2025版使用较多同义表述。每份设有明确否定句，以及下一年度研发投入目标6.50%作为时间/目标性质干扰项。目标值不列入实际值答案。
正文主要是连续叙述段落，并非把答案表复制进Word；两年数值独立模拟，不保证每项都改善。航空/机场、化工多产品及建材多产品明确分板块边界。

评测建议：先对DOCX段落分句、中文分词，再以字典名及同义词构建TF-IDF向量或语义向量，用余弦相似度召回候选句；随后用数字/单位规则识别值，处理否定、年份、目标/实际、统计范围。词频或余弦相似度本身不能完成可靠数值抽取。建议逐项核对指标、取值、单位、年份和证据，计算精确率/召回率；不能把答案表或本生成脚本当作模型输入。
两年的报告采用相近写作模板，不能将其视作独立真实企业语料或宣称跨文体泛化；更严格实验应按行业/模板分组留出。未来应补充真实授权材料、OCR噪声及遗漏样本。

字典口径提示：
1. 第39行指标名称中的炼油单位是kg oe/(t·能量因数)，但单位栏仅写t·能量因数。正文使用完整单位，答案同时保留原字典单位。
2. 第46行水泥熟料综合能耗标杆为1 kgce/t，数值可疑。本语料使用103–118 kgce/t的模拟实绩，不将该标杆当作真实标准，建议另行核实字典。
3. 字典没有企业经营质量评分公式，报告直接给出模拟分值，不擅自由财务比率计算。
4. 企业经营质量等字段虽曾在系统中调整，本次按用户提供的字典完整生成；导入系统前需另做字段映射。

文件结构：报告正文/下按行业存放DOCX；评测答案/为独立CSV及JSON。请向算法测试者仅分发报告正文，标准答案留给评测方。
'''
    (ROOT/'使用说明.txt').write_text(readme,encoding='utf-8-sig')
    # 结构覆盖与证据复核：每个字典适用项恰好出现一次，正文包含对应证据段落。
    for path in REPORTS.rglob('*.docx'):
        matching=[a for a in answers if a['file']==path.relative_to(ROOT).as_posix()]
        text='\n'.join(p.text for p in Document(path).paragraphs)
        assert len(matching)==13+sum(r[0]==path.parent.name for r in rows[1:])
        assert len({a['dictionary_row'] for a in matching})==len(matching)
        assert all(a['evidence'] in text for a in matching)
    package = ROOT.parent/'行业指标提取Word语料.zip'
    with zipfile.ZipFile(package,'w',zipfile.ZIP_DEFLATED) as z:
        for path in ROOT.rglob('*'):
            if path.is_file(): z.write(path, path.relative_to(ROOT))
    print(json.dumps({'documents':len(list(REPORTS.rglob('*.docx'))),'answers':len(answers),'zip':str(package)},ensure_ascii=False))

if __name__ == '__main__': main()
