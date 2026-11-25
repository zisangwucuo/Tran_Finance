"""复杂叙述语料：保留原版，按业务主题混排事实，单独记录干扰项与证据。"""
import csv
import json
import random
import zipfile
from pathlib import Path
from docx import Document
from generate_indicator_reports import new_doc, SCOPES, industries, GOV, ALIASES

BASE = Path(__file__).resolve().parents[1] / 'output'
ROOT = BASE / '行业指标提取Word语料_复杂叙述版'
SOURCE = json.loads((BASE/'行业指标提取Word语料/评测答案/指标标准答案.json').read_text(encoding='utf-8'))
TOPICS = [
 ('经营回顾与市场变化', '客户订单在年内呈现明显的批次差异。销售部门将交付周期、结算条件和售后响应分开管理，没有单纯以签约金额评价客户质量。部分长期客户缩短了采购计划的锁定窗口，运营部门因此按月调整生产节奏。对于临时追加订单，管理层要求先落实设备、库存与运输资源，再确认交付日期。', '在客户回访中，稳定供货、质量一致性和可追溯记录仍是讨论较多的事项。低碳产品的询价有所增加，但客户愿意接受的溢价尚不稳定。集团没有把询价意向全部认定为已实现收入，框架协议也需要在实际交付后才能结算。'),
 ('组织运行与人员安排', '年度组织调整主要围绕生产、财务和采购之间的信息衔接展开。过去同一项目在不同部门使用不同编号，付款、验收和设备登记容易出现时间差。本期开始使用统一项目编号，但历史记录仍需逐笔核对。管理层强调，流程上线不等于流程已经有效运行，抽查发现的问题应由业务负责人回复。', '部分岗位采用师带徒和轮岗方式补充经验。内部培训同时涵盖产品质量、信息系统和现场安全，并非所有培训都属于低碳转型专项。人力资源部门把参加人数与完成考核人数分别登记，外包人员也单列统计，避免直接套用全体员工口径。'),
 ('生产协同与检修记录', '年度检修安排需要与客户交付计划协调。运行团队将计划停机、临时检修和低负荷运行分别记录，以免把停机期间的低消耗误判为设备效率改善。个别装置完成维护后仍有一段调试期，调试数据只用于工艺诊断，没有直接并入正常运行的最佳工况样本。', '技术人员发现，部分仪表在高负荷和低负荷条件下的偏差并不相同。生产例会要求对异常数据保留原记录，不能仅删除不利数值。月度报表中的短期改善需结合产量、产品结构和检修安排解释，年末汇总则按照实际业务量加权处理。'),
 ('供应链与项目执行', '采购团队在选择设备和服务商时同时考察交期、备件供应和现场服务能力。报价最低的方案不一定具有最低的全生命周期成本。对于首次合作供应商，集团先采用小批次试用，再根据验收结果确定是否扩大合作。供应商的产品宣传资料仅作为技术交流文件，不代替本企业实测。', '工程进度与财务付款进度并不完全同步。已签订合同可能尚未开工，已到货设备也可能没有形成可运行资产。项目台账区分合同额、当期支付额和资本化金额，预算调整需要重新履行审批。年末仍在论证的项目没有被写成已完成投资或实际产出。'),
 ('财务复盘与研发项目', '财务复盘关注回款周期、存货结构及费用归属。部分客户在验收完成后才支付尾款，现金回收时间可能晚于收入确认。研发项目中的设备采购、试验耗材和外部测试费用按会计口径分类处理，不能把资本性支出全部当作当期费用，也不能把资金预算视作实际支出。', '研发部门保留了试验失败的阶段记录。小试结果达到预期后仍需进行中试，不能直接推断大规模生产的经济效果。管理层要求把技术完成度、知识产权和现场应用情况分别说明；专利申请数量并不等于已经投产的转型技术数量。'),
 ('资源环境与运行边界', '环境数据来自不同装置和不同计量周期。能源购入、内部回收和对外输出需分别登记，跨基地调拨也要消除重复统计。排放总量与单位产品强度采用不同分母，相关部门在汇总时保留计算底稿。采购凭证能够证明交易，但不能单独证明能源已在当年实际消耗。', '现场检查还关注噪声、一般固废和厂区道路扬尘。部分治理措施改善了现场环境，却不会直接反映在碳强度指标中。报告不将所有环保投入合并解释为减碳投入，也不以获得某项内部表扬替代正式环保绩效等级。'),
 ('专项业务观察', '不同业务单元的产品和服务边界存在差异。集团层面的经营讨论侧重资源调配，而具体技术指标仍在对应生产单元核算。对于单位相同但分母定义不同的数据，统计人员保留备注，不以简单平均形成集团值。分包或委外业务按报告边界另行说明。', '在技术交流中，经常出现最佳工况、设计参数和实际运行值同时列示的情况。设计参数用于设备选型，验收试验用于确认合同要求，全年运行数据则用于回顾实际绩效。三类数据均有业务意义，但不能因为某个数值更好看就互相替换。'),
 ('年度议题与后续工作', '下一阶段工作将重点放在信息衔接和项目跟踪上。管理层要求各部门在提交计划时同时说明实施条件、负责人和风险事项。董事会讨论的目标在正式批准前仍属于方案，不构成对外承诺；已批准项目也可能因施工窗口和供应条件变化调整执行节奏。', '本次回顾没有对所有事项作出完成结论。对于仍需补充证据的工作，责任部门继续保留问题清单，并安排在后续例会复核。对外交流材料与内部经营分析的口径应保持一致，但涉及不同统计范围时必须保留原始说明，不通过删去不利信息制造一致性。')
]
PROJECTS = ['高炉炉顶余压回收与电炉供电改造','风光场站并网与煤电调峰协同','氨醇联产装置换热网络优化','炼油加热炉检修与生物燃料试生产','熟料窑余热回收与玻璃窑炉检修','铝液输送与再生铝分选系统改造','矿井排水泵改造与瓦斯利用管线维护','机坪车辆更新与航班地面保障协同']

# 每个行业的最终文档数量不同。既有两份保留原文件名，新报告使用不同主体，
# 避免仅改年份形成互相矛盾的连续年度数据；随机种子固定，方便复现实验。
COUNTS = dict(zip(industries, [15,13,11,9,7,12,8,14]))
NAMES = ['景禾','承泽','锦川','远晟','润衡','明岑','启瀚','嘉岳','宜川','卓宁','沐辰','朗清','丰屹']

def expanded_source():
    result=[dict(item, company=item['company'].replace('示例','')) for item in SOURCE]
    for ix, industry in enumerate(industries):
        for number in range(COUNTS[industry]-2):
            year=2023+number%3
            company=NAMES[number]+industry.replace('行业','')+'集团'
            rng=random.Random(84013+ix*313+number*43)
            template=[item for item in SOURCE if item['industry']==industry and item['year']==2024+number%2]
            file=f'报告正文/{industry}/{year}年{company}经营与转型报告.docx'
            for index,item in enumerate(template):
                a=dict(item,company=company,year=year,file=file)
                if index<9:
                    a['value']=int(rng.random()>.23)
                elif isinstance(a['value'],(int,float)):
                    value=round(a['value']*rng.uniform(.79,1.21),3)
                    if a['unit']=='%': value=max(-99,min(99.5,value))
                    if index==9: value=max(.05,min(.98,value))
                    a['value']=value
                else:
                    a['value']=rng.choice(['A','B','C'])
                result.append(a)
    return result

def main():
    ROOT.mkdir(parents=True, exist_ok=True)
    answer_dir=ROOT/'评测答案'; answer_dir.mkdir(exist_ok=True)
    answers, decoys, stats = [], [], []
    corpus=expanded_source()
    for document_index,file in enumerate(dict.fromkeys(a['file'] for a in corpus)):
        original=[dict(a) for a in corpus if a['file']==file]
        industry, year, company = original[0]['industry'], original[0]['year'], original[0]['company']
        ix=industries.index(industry); rng=random.Random(7319+ix*101+year+document_index*71)
        doc=new_doc(f'{year}年{industry}经营与转型报告',company,year)
        # 正文按正式报告排版；合成来源移至独立说明与文件属性，避免成为提取提示词。
        disclaimer=doc.paragraphs[-1]._element
        disclaimer.getparent().remove(disclaimer)
        doc.core_properties.comments='合成研究语料；企业、事件和数据并非真实企业披露。'
        doc.sections[0].header.paragraphs[0].text=f'{company} 年度经营回顾'
        footer=doc.sections[0].footer.paragraphs[0]
        for run in footer.runs:
            run.text=run.text.replace('模拟材料 · 非真实企业披露','年度经营报告')
        scope=SCOPES[ix].replace('本虚构集团','本集团').replace('示例边界','统计边界')
        doc.add_paragraph(f'本报告回顾{company}截至{year}年12月31日的经营事项。除另行标注的期间、主体或试验边界外，年度统计均指{year}年。'+scope)
        buckets=[[] for _ in TOPICS]
        for a in original:
            name=a['indicator']; j=original.index(a)
            if j<9:
                phrase=GOV[j][1 if a['value'] else 2]
                sentence=f'截至本年末，集团{phrase}。'
                bucket=[7,7,1,2,5,0,7,1,5][j]
            elif j<13:
                mention=['经营质量评价得分','销售净利率','营业收入同比增长率','研发支出占营业收入比重'][j-9]
                suffix='分（0至1区间的独立评价结果）' if j==9 else '%'
                sentence=f'财务复盘材料记录，本年度{mention}为{a["value"]}{suffix}。'
                bucket=4 if j!=11 else 0
            else:
                mention=ALIASES[industry][j-13] if year==2025 or j%2==0 else a['mention']
                suffix='' if a['unit']=='等级' else ' '+a['unit']
                sentence=rng.choice([f'全年台账汇总后，{mention}最终登记为{a["value"]}{suffix}。',
                    f'业务复核将{mention}的本年实际结果确认在{a["value"]}{suffix}，相关底稿已归档。',
                    f'在正常年度统计边界内，{mention}本期为{a["value"]}{suffix}。'])
                bucket=2 if '能耗' in name or '电耗' in name else 5 if '评级' in name or '排放' in name else 6
            a['evidence']=sentence; a['section']=TOPICS[bucket][0]; a['mention']=phrase if j<9 else mention
            buckets[bucket].append((sentence,a))
        section_order=list(range(1,7)); rng.shuffle(section_order)
        for display_index,section_index in enumerate([0]+section_order+[7],1):
            heading,intro,closing=TOPICS[section_index]
            doc.add_paragraph(f'{display_index} {heading}','Heading 1')
            doc.add_paragraph(intro)
            project=PROJECTS[ix]
            n=rng.randint(8,37); amount=round(rng.uniform(280,1800),2)
            noise=rng.choice([
                f'围绕{project}，本期组织了{n}次专题沟通，形成{n+12}份过程记录。其中一项辅助设施合同预算为{amount}万元，计划分批验收。现场团队在周会上讨论了交付时间、人员协调和备件保障，部分事项需要供应商再次确认后才能关闭。',
                f'档案复核涉及{n+12}份交接记录，{n}项附件已完成签收。与{project}配套的仓储及道路工程预算为{amount}万元，尚有两批物料等待到货。采购部门安排驻场协调人员，按交付节点核对技术附件和保修责任。',
                f'本期客户与供应商沟通安排覆盖{n}场会议。{project}相关的辅助设施计划投入{amount}万元，财务部门分期办理付款审核，项目组整理了{n+12}份联系单。个别供货批次调整运输路线，现场验收与资料归档继续由不同岗位分别负责。'])
            doc.add_paragraph(noise)
            decoys.append(dict(file=file,kind='非目标经营数字',evidence=noise,reason='会议数量、记录数量和单项合同预算不是字典目标指标'))
            entries=buckets[section_index]; rng.shuffle(entries)
            # 一段混合多项事实与普通经营叙述，避免一个指标固定对应一个段落。
            for offset in range(0,len(entries),2):
                group=entries[offset:offset+2]
                paragraph='例会并未只围绕单一指标展开。'+group[0][0]
                paragraph+='同期，业务人员还复查了合同附件和计量凭证，发现部分签字日期晚于实际业务发生日期，需要补充说明。'
                if len(group)>1: paragraph+=group[1][0]
                paragraph+='有关讨论形成的材料由业务和统计人员共同留档，部门简报仅保留主要结论。'
                doc.add_paragraph(paragraph)
                for sentence,a in group:
                    a['evidence_paragraph']=paragraph; answers.append(a)
            if section_index in [0,2,4,5,6,7]:
                candidates=[a for a in original if isinstance(a['value'],(float,int)) and a['unit']!='0/1']
                a=candidates[(section_index+ix)%len(candidates)]
                value=round(float(a['value'])*1.08+.01,3)
                if a['unit']=='%': value=min(value,99.9)
                if a['unit']=='分(0-1)': value=min(value,.99)
                unit=a['unit']; mention=a['mention']
                kind=['历史年度','未来目标','局部试点'][section_index%3]
                if kind=='历史年度':
                    previous=next((item for item in corpus if item['company']==company and item['industry']==industry and item['year']==year-1 and item['indicator']==a['indicator']),None)
                    if previous is not None:
                        value=previous['value']
                if kind=='历史年度': text=f'资料室保留的{year-1}年同期简报中，{mention}曾记录为{value} {unit}。本次编制未将这份旧简报作为本年实绩；当时的生产组织与本期并不相同。'
                elif kind=='未来目标': text=f'供内部讨论的{year+1}年预算草案，将{mention}暂列为{value} {unit}。该方案仍需结合资源条件复核，尚未形成实际完成记录。'
                else: text=f'另有一份本年专项测试纪要记载，东区试点单元在连续72小时观察中，{mention}为{value} {unit}。这次试点仅涉及局部装置和短期运行窗口，不能替代全年正常统计。'
                doc.add_paragraph(text); decoys.append(dict(file=file,kind=kind,evidence=text,reason='年份、实际/目标性质或统计范围不符合当年全年目标口径'))
            doc.add_paragraph(closing)
            doc.add_paragraph(rng.choice([
                '管理层在复核中提出，应把问题发生的时间、处理过程和最终状态分别记录。部分事项虽已安排负责人，但尚未完成闭环；会议纪要中的“推进”和“研究”不能直接理解为工作已经完成。跨部门事项下次复盘时仍需核对实际执行情况。',
                '对同一事项，不同部门有时使用简称或沿用旧项目名称。档案管理人员在整理时保留了这些表达，并通过合同编号和业务单元识别对应关系。对于仍有争议的统计边界，相关人员记录了双方意见，没有为了简化报告而直接合并数据。']))
        path=ROOT/file; path.parent.mkdir(parents=True,exist_ok=True); doc.save(path)
        text='\n'.join(p.text for p in Document(path).paragraphs)
        selected=[a for a in answers if a['file']==file]
        assert len(selected)==len(original) and all(a['evidence'] in text for a in selected)
        assert all(item['evidence'] in text for item in decoys if item['file']==file)
        assert '附注' not in text and '模拟' not in text
        stats.append(dict(file=file,characters=len(text),targets=len(selected)))
    for name,data in [('指标标准答案',answers),('干扰项标注',decoys)]:
        (answer_dir/f'{name}.json').write_text(json.dumps(data,ensure_ascii=False,indent=2),encoding='utf-8')
        with (answer_dir/f'{name}.csv').open('w',encoding='utf-8-sig',newline='') as f:
            writer=csv.DictWriter(f,fieldnames=list(data[0]));writer.writeheader();writer.writerows(data)
    (ROOT/'使用说明.txt').write_text('复杂叙述版：16份模拟Word报告，完整覆盖328项适用指标答案。\n正文按业务主题组织，不按字典顺序逐项罗列；含混合段落、年度/局部/目标数字、非目标经营数字和一般叙述。所有事实为合成，不代表真实企业披露。\n请只将报告正文交给提取程序，评测答案独立保留。标准答案提供短证据及完整段落，干扰项标注提供排除理由。词频/余弦相似度用于候选召回，数值抽取仍须判断年份、单位、主体、否定与范围。\n保留原版字典口径说明：炼油单位采用完整kg oe/(t·能量因数)；水泥熟料标杆1需核实；经营质量采用直接披露评分。\n本版保留原版模拟实际值以便比较提取难度，往年干扰值仅为独立情境设定，不保证跨文档时间序列一致，不能用于纵向经营分析。模板仍为合成写作，不应以本集合评测结果声称对真实年报的泛化能力。\n',encoding='utf-8-sig')
    (answer_dir/'覆盖检查.json').write_text(json.dumps(stats,ensure_ascii=False,indent=2),encoding='utf-8')
    (ROOT/'使用说明.txt').write_text(
        f'复杂叙述版扩充语料：共{len(stats)}份Word报告，{len(answers)}项指标答案。\n'
        +'\n'.join(f'{industry}：{count}份' for industry,count in COUNTS.items())
        +'\n\n来源说明：本集合为合成研究语料，企业名称、经营事项与数值均为生成内容，不是真实企业披露。正文采用正式报告口吻，移除了附注及重复的模拟提示；不得将生成材料标注为真实年报或真实调研证据。\n'
        +'仅将报告正文交给提取程序。评测答案和干扰项标注独立保存，不作为模型输入。各报告覆盖所属行业的通用与专项指标，保留往年、未来目标、局部测试及非目标经营数字。\n'
        +'2024与2025同主体引用保持对应；新增报告采用不同主体。炼油单位使用完整单位，熟料标杆口径仍需另行核实。企业经营质量为直接披露的合成评价值。\n',encoding='utf-8-sig')
    with zipfile.ZipFile(BASE/'行业指标提取Word语料_复杂叙述版.zip','w',zipfile.ZIP_DEFLATED) as z:
        for p in ROOT.rglob('*'):
            if p.is_file():z.write(p,p.relative_to(ROOT))
    print(json.dumps(dict(documents=len(stats),characters=[min(x['characters'] for x in stats),max(x['characters'] for x in stats)],answers=len(answers),decoys=len(decoys)),ensure_ascii=False))

if __name__=='__main__': main()
